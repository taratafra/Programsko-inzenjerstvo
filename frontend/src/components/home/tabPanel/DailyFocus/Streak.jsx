import { useState, useEffect } from 'react';
import styles from './MoodHabits.module.css';

export default function Streak({user, getAccessTokenSilently}) {
  const BACKEND_URL = process.env.REACT_APP_BACKEND;

  const [streak, setStreak] = useState({
    currentStreak: 0,
    longestStreak: 0,
    lastCompletedDate: null
  });
  const [badges, setBadges] = useState([]);
  const [question, setQuestion] = useState(null);
  const [answer, setAnswer] = useState('');
  const [completedToday, setCompletedToday] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Funkcija za dobivanje tokena
  const getToken = async () => {
    try {
      // OAuth
      if (getAccessTokenSilently) {
        const token = await getAccessTokenSilently({
          authorizationParams: {
            audience: process.env.REACT_APP_AUTH0_AUDIENCE,
          },
        });
        return token;
      }
      
      // Fallback na localStorage
      return localStorage.getItem('token');
    } catch (error) {
      console.error('Error getting token:', error);
      return localStorage.getItem('token');
    }
  };

  useEffect(() => {
    fetchAllData();
  },[]);   //uglate zagrade = ovo samo jednom ucita

  const fetchAllData = async () => {
    setLoading(true);
    try {
      const token = await getToken();
      
      if (!token) {
        console.error('No token available');
        setLoading(false);
        return;
      }

      // Fetch pitanje i status
      const dailyFocusResponse = await fetch(`${BACKEND_URL}/api/daily-focus/me`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (dailyFocusResponse.ok) {
        const dailyData = await dailyFocusResponse.json();
        // Backend vraća questions array i completed boolean
        const firstQuestion = dailyData.questions && dailyData.questions.length > 0 
          ? dailyData.questions[0] 
          : null;
        setQuestion(firstQuestion);
        setCompletedToday(dailyData.completed || false);
      }
      
      // Fetch streak
      const streakResponse = await fetch(`${BACKEND_URL}/api/daily-focus/me/streak`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (streakResponse.ok) {
        const streakData = await streakResponse.json();
        setStreak({
          currentStreak: streakData.currentStreak || 0,
          longestStreak: streakData.longestStreak || 0,
          lastCompletedDate: streakData.lastCompletedDate || null
        });
      }

      // Fetch badges
      const badgesResponse = await fetch(`${BACKEND_URL}/badges/me`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (badgesResponse.ok) {
        const badgesData = await badgesResponse.json();
        setBadges(badgesData);
      }
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitAnswer = async () => {
    if (!answer.trim() || !question) return;

    setSubmitting(true);
    try {
      const token = await getToken();
      
      if (!token) {
        alert('Authentication error. Please log in again.');
        setSubmitting(false);
        return;
      }
      
      // Šalji odgovor na backend
      const response = await fetch(`${BACKEND_URL}/api/daily-focus/me/complete`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          answers: {
            [question.id]: answer
          }
        })
      });

      if (response.ok) {
        setCompletedToday(true);
        setAnswer('');
        // Refresh streak i badges nakon submita
        fetchAllData();
      } else {
        console.error('Failed to submit answer');
        alert('Failed to submit answer. Please try again.');
      }
    } catch (err) {
      console.error('Error submitting answer:', err);
      alert('Error submitting answer. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const getBadgeIcon = (badgeType) => {
    switch(badgeType) {
      case 'STREAK_7':
        return '🥉';
      case 'STREAK_30':
        return '🥈';
      case 'STREAK_100':
        return '🥇';
      default:
        return '🏆';
    }
  };

  const getBadgeName = (badgeType) => {
    switch(badgeType) {
      case 'STREAK_7':
        return '7 Day Streak';
      case 'STREAK_30':
        return '30 Day Streak';
      case 'STREAK_100':
        return '100 Day Streak';
      default:
        return 'Achievement';
    }
  };

  if (loading) {
    return (
      <div className={styles.container}>
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div>
      {/*question*/}
      {!completedToday && question ? (
        <div className={styles.questionContainer}>
          <h3 className={styles.questionTitle}>Daily Focus</h3>
          <p className={styles.question}>{question.text}</p>
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder="Write your answer..."
            rows={4}
            className={styles.textarea}
            disabled={submitting}
          />
          <button
            className={styles.submitButton}
            onClick={handleSubmitAnswer}
            disabled={!answer.trim() || submitting}
          >
            {submitting ? 'Submitting...' : 'Submit'}
          </button>
        </div>
      ) : completedToday ? (
        <div className={styles.completedMessage}>
          <p>You've completed today's daily focus! Come back tomorrow for a new question.</p>
        </div>
      ) : null}

      {/* divider */}
      <div className={styles.divider}></div>

      {/*streak*/}
      <h3 className={styles.title}>Current Streak</h3>
      <div className={styles.streakContainer}>
        <div className={styles.streakBadge}>
          <span className={styles.streakIcon}>🔥</span>
          <div className={styles.streakInfo}>
            <span className={styles.streakNumber}>{streak.currentStreak}</span>
            <span className={styles.streakLabel}>day streak</span>
          </div>
        </div>

        {streak.longestStreak > streak.currentStreak && (
          <div className={styles.longestStreak}>
            <span>Best: {streak.longestStreak} days</span>
          </div>
        )}
      </div>

      {/*divider */}
      <div className={styles.divider}></div>

      {/*badges */}
      {badges.length > 0 ? (
        <div>
          <h3 className={styles.title}>Your Achievements</h3>
          <div className={styles.badgesGrid}>
            {badges.map((badge) => (
              <div key={badge.id} className={styles.badgeItem}>
                <span className={styles.BadgeIcon}>{getBadgeIcon(badge.badgeType)}</span>
                <span className={styles.BadgeName}>{getBadgeName(badge.badgeType)}</span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className={styles.noBadges}>
          <p>Build streaks to earn badges!</p>
        </div>
      )}
    </div>
  );
}