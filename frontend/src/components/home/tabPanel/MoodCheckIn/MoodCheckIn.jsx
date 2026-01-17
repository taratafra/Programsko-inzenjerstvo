import React, { useState, useEffect } from 'react';
import styles from '../DailyFocus/MoodHabits.module.css';

export default function MoodCheckIn({getAccessTokenSilently, isAuthenticated }) {
  const BACKEND_URL = process.env.REACT_APP_BACKEND;

  const [formData, setFormData] = useState({
    moodScore: null,
    emotions: [],
    notes: '',
    sleepQuality: null,
    stressLevel: null,
    focusLevel: null,
    caffeineIntake: '',
    alcoholIntake: '',
    physicalActivity: '',
  });

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [canSubmitToday, setCanSubmitToday] = useState(true);

  const emotionOptions = [
    { value: 'HAPPY', label: '😊 Happy' },
    { value: 'SAD', label: '😢 Sad' },
    { value: 'ANXIOUS', label: '😰 Anxious' },
    { value: 'CALM', label: '😌 Calm' },
    { value: 'ENERGETIC', label: '⚡ Energetic' },
    { value: 'TIRED', label: '😴 Tired' },
    { value: 'FRUSTRATED', label: '😤 Frustrated' },
    { value: 'GRATEFUL', label: '🙏 Grateful' },
  ];

  const getToken = async () => {
  try {
    // Auth0 flow
    if (isAuthenticated && getAccessTokenSilently) {
      const token = await getAccessTokenSilently({
        authorizationParams: {
          audience: process.env.REACT_APP_AUTH0_AUDIENCE,
        },
      });
      return token; 
    }

    // fallback (ako je custom login)
    return localStorage.getItem('token');
  } catch (err) {
    console.error('Error getting token:', err);
    return null;
  }
};

  useEffect(() => {
    checkSubmissionStatus();
  }, []);

  const checkSubmissionStatus = async () => {
    try {
      const today = new Date().toISOString().split('T')[0];
      const token = await getToken();

      const response = await fetch(
        `${BACKEND_URL}/api/mood-checkins/me/${today}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (response.status === 404) {
        setCanSubmitToday(true);
        return;
      }

      if (response.ok) {
        setCanSubmitToday(false);
      }
    } catch {
      setCanSubmitToday(true);
    }
  };

  const handleMoodClick = (score) => {
    setFormData({ ...formData, moodScore: score });
  };

  const handleEmotionToggle = (emotion) => {
    setFormData({
      ...formData,
      emotions: formData.emotions.includes(emotion)
        ? formData.emotions.filter(e => e !== emotion)
        : [...formData.emotions, emotion],
    });
  };

  const handleSliderChange = (field, value) => {
    setFormData({ ...formData, [field]: parseInt(value) });
  };

  const isFormValid = () => 
      formData.moodScore !== null 

  const handleSubmit = async () => {
    if (!isFormValid()) {
      setError('Fields marked with * are required.');
      return;
    }    
    if (!canSubmitToday) {
      setError('You have already submitted your mood check-in today.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const token = await getToken();

      const response = await fetch(`${BACKEND_URL}/api/mood-checkins/me`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
          body: JSON.stringify({
            moodScore: formData.moodScore,
            emotions: formData.emotions,
            ...(formData.sleepQuality !== null && {
              sleepQuality: formData.sleepQuality,
            }),
            ...(formData.stressLevel !== null && {
              stressLevel: formData.stressLevel,
            }),
            ...(formData.focusLevel !== null && {
              focusLevel: formData.focusLevel,
            }),
            ...(formData.caffeineIntake && {
              caffeineIntake: formData.caffeineIntake,
            }),
            ...(formData.alcoholIntake && {
              alcoholIntake: formData.alcoholIntake,
            }),
            ...(formData.physicalActivity && {
              physicalActivity: formData.physicalActivity,
            }),
            ...(formData.notes && { notes: formData.notes }),
          }),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error('Error response:', errorData);
        throw new Error(errorData.message || 'Failed to submit mood check-in');
      }

      setSuccess(true);
      setCanSubmitToday(false);

    } catch (err) {
      console.error('Submit error:', err);
      setError(err.message || 'Failed to submit. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (success || !canSubmitToday && !success) {
    return (
      <div className={styles.container}>
        <h2 className={styles.completedTitle}>Successfully submitted!</h2>
        <p className={styles.completedSubtext}>
          You can track your progress through statistics.
        </p>
      </div>
    );
  }

  return (
  <div >
    <h1 className={styles.title}>Daily Mood Check-in</h1>
    <div className={styles.formContent}>          
      {/* Mood Score */}
      <div className={styles.section}>
        <label className={styles.label}>
          How are you feeling today? *
        </label>
        <div className={styles.moodButtons}>
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((score) => (
            <button
              key={score}
              onClick={() => handleMoodClick(score)}
              className={`${styles.moodButton} ${
                formData.moodScore === score ? styles.moodButtonActive : ''
              }`}
            >
              {score}
            </button>
          ))}
        </div>
        <div className={styles.scaleLabels}>
          <span>Very Bad</span>
          <span>Excellent</span>
        </div>
      </div>

      {/* Emotions */}
      <div className={styles.section}>
        <label className={styles.label}>
          What emotions are you experiencing?
        </label>
        <div className={styles.emotionGrid}>
          {emotionOptions.map((emotion) => (
            <button
              key={emotion.value}
              onClick={() => handleEmotionToggle(emotion.value)}
              data-emotion={emotion.value}
              className={`${styles.emotionButton} ${
                formData.emotions.includes(emotion.value) ? styles.emotionButtonActive : ''
              }`}
            >
              <span className={styles.emotionEmoji}>
                {emotion.label.split(' ')[0]}
              </span>
              <span className={styles.emotionLabel}>
                {emotion.label.split(' ')[1]}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Sleep Quality */}
      <div className={styles.section}>
        <label className={styles.label}>
          Sleep Quality
        </label>
        <input
          type="range"
          min="1"
          max="10"
          value={formData.sleepQuality || 5}
          onChange={(e) => handleSliderChange('sleepQuality', e.target.value)}
          className={styles.slider}
        />
        <div className={styles.sliderLabels}>
          <span>Poor</span>
          {formData.sleepQuality && (
            <span className={styles.sliderValue}>{formData.sleepQuality}/10</span>
          )}
          <span>Excellent</span>
        </div>
      </div>

      {/* Stress Level */}
      <div className={styles.section}>
        <label className={styles.label}>
          Stress Level
        </label>
        <input
          type="range"
          min="1"
          max="10"
          value={formData.stressLevel || 5}
          onChange={(e) => handleSliderChange('stressLevel', e.target.value)}
          className={styles.slider}
        />
        <div className={styles.sliderLabels}>
          <span>No Stress</span>
          {formData.stressLevel && (
            <span className={styles.sliderValue}>{formData.stressLevel}/10</span>
          )}
          <span>Very Stressed</span>
        </div>
      </div>

      {/* Focus Level */}
      <div className={styles.section}>
        <label className={styles.label}>
          Focus Level
        </label>
        <input
          type="range"
          min="1"
          max="10"
          value={formData.focusLevel || 5}
          onChange={(e) => handleSliderChange('focusLevel', e.target.value)}
          className={styles.slider}
        />
        <div className={styles.sliderLabels}>
          <span>Distracted</span>
          {formData.focusLevel && (
            <span className={styles.sliderValue}>{formData.focusLevel}/10</span>
          )}
          <span>Very Focused</span>
        </div>
      </div>

      {/* Caffeine Intake */}
      <div className={styles.section}>
        <label className={styles.label}>
          Caffeine Intake
        </label>
        <input
          type="text"
          placeholder="e.g., 2 cups of coffee"
          value={formData.caffeineIntake}
          onChange={(e) => setFormData({ ...formData, caffeineIntake: e.target.value })}
          className={styles.textInput}
        />
      </div>

      {/* Alcohol Intake */}
      <div className={styles.section}>
        <label className={styles.label}>
          Alcohol Intake
        </label>
        <input
          type="text"
          placeholder="e.g., 1 glass of wine"
          value={formData.alcoholIntake}
          onChange={(e) => setFormData({ ...formData, alcoholIntake: e.target.value })}
          className={styles.textInput}
        />
      </div>

      {/* Physical Activity */}
      <div className={styles.section}>
        <label className={styles.label}>
          Physical Activity
        </label>
        <input
          type="text"
          placeholder="e.g., 30 min walk, gym session"
          value={formData.physicalActivity}
          onChange={(e) => setFormData({ ...formData, physicalActivity: e.target.value })}
          className={styles.textInput}
        />
      </div>

      {/* Notes */}
      <div className={styles.section}>
        <label className={styles.label}>
          Additional Notes
        </label>
        <textarea
          placeholder="How was your day? Any thoughts or reflections..."
          value={formData.notes}
          onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
          rows="4"
          className={styles.textarea}
        />
      </div>

      {/* Error Message */}
      {error && (
        <div className={styles.errorMessage}>
          {error}
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div className={styles.successMessage}>
          Successfully submitted!
        </div>
      )}

      {/* Submit Button */}
      <button
        onClick={handleSubmit}
        disabled={submitting}
        className={`${styles.submitButton} ${submitting ? styles.submitButtonDisabled : ''}`}
      >
        {submitting ? 'Submitting...' : 'Submit Check-in'}
      </button>
    </div>
  </div>
  );
}
