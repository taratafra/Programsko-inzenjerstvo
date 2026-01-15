import React, { useState, useEffect } from 'react';

import HomeStyles from "../../../../pages/Home/Home.module.css"; // koristi isti CSS kao tabPanel
import styles from './MoodHabits.module.css';

export default function MoodHabits({ user }) {
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
  const [lastSubmission, setLastSubmission] = useState(null);
  const [canSubmitToday, setCanSubmitToday] = useState(true);

  const emotionOptions = [
    { value: 'happy', label: '😊 Happy', color: '#FFD700' },
    { value: 'sad', label: '😢 Sad', color: '#4682B4' },
    { value: 'anxious', label: '😰 Anxious', color: '#FF6347' },
    { value: 'calm', label: '😌 Calm', color: '#98FB98' },
    { value: 'energetic', label: '⚡ Energetic', color: '#FFA500' },
    { value: 'tired', label: '😴 Tired', color: '#9370DB' },
    { value: 'frustrated', label: '😤 Frustrated', color: '#DC143C' },
    { value: 'grateful', label: '🙏 Grateful', color: '#FF69B4' },
  ];

  useEffect(() => {
    checkSubmissionStatus();
  }, []);

  const checkSubmissionStatus = async () => {
    try {
      const response = await fetch('/api/mood-checkins/me', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      const data = await response.json();
      setCanSubmitToday(!data.submittedToday);
      setLastSubmission(data.lastSubmissionDate);
    } catch (err) {
      console.error('Error checking submission status:', err);
    }
  };

  const handleMoodClick = (score) => {
    setFormData({ ...formData, moodScore: score });
  };

  const handleEmotionToggle = (emotion) => {
    const updatedEmotions = formData.emotions.includes(emotion)
      ? formData.emotions.filter(e => e !== emotion)
      : [...formData.emotions, emotion];
    setFormData({ ...formData, emotions: updatedEmotions });
  };

  const handleSliderChange = (field, value) => {
    setFormData({ ...formData, [field]: parseInt(value) });
  };

  const isFormValid = () => {
    return (
      formData.moodScore !== null ||
      formData.emotions.length > 0 ||
      formData.notes.trim() !== '' ||
      formData.sleepQuality !== null ||
      formData.stressLevel !== null ||
      formData.focusLevel !== null ||
      formData.caffeineIntake !== '' ||
      formData.alcoholIntake !== '' ||
      formData.physicalActivity !== ''
    );
  };

  const handleSubmit = async () => {
    if (!isFormValid()) {
      setError('Please answer at least one question before submitting.');
      return;
    }

    if (!canSubmitToday) {
      setError('You have already submitted your mood check-in today.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const response = await fetch('/api/mood-checkins/me', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          ...formData,
          date: new Date().toISOString(),
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to submit mood check-in');
      }

      const result = await response.json();
      setSuccess(true);
      setCanSubmitToday(false);

    } catch (err) {
      setError(err.message || 'Failed to submit. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!canSubmitToday && !success) {
    return (
      <div className={styles.container}>
          <h2 className={styles.completedTitle}>Check-in Complete!</h2>
          <p className={styles.completedSubtext}>
            Come back tomorrow to continue your streak!
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
              How are you feeling today?
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

          <p className={styles.footerNote}>
            * At least one question must be answered
          </p>
        </div>
      </div>
  );
}