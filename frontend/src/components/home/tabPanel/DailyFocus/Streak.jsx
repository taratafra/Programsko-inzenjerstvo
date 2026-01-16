import React, { useState, useEffect } from 'react';
import styles from './MoodHabits.module.css';

export default function Streak({ getAccessTokenSilently, isAuthenticated }) { 
  const BACKEND_URL = process.env.REACT_APP_BACKEND;

  const [streak, setStreak] = useState([]);
  const [badges, setBadges] = useState([]);
  const [loading, setLoading] = useState(true);

  const getToken = async () => {
    try {
      // Auth0 flow
      if (isAuthenticated && getAccessTokenSilently) {
        return await getAccessTokenSilently({
          authorizationParams: { audience: process.env.REACT_APP_AUTH0_AUDIENCE },
        });
      }
      return localStorage.getItem('token');
    } catch (err) {
      console.error('Error getting token:', err);
      return null;
    }
  };

  useEffect(() => {
    fetchStreakAndBadges();
  }, [isAuthenticated]); 

  const fetchStreakAndBadges = async () => {
    setLoading(true);
    
    try {
      const token = await getToken();

      if (!token) {
        console.warn('No token available, skipping streak fetch');
        return;
      }

      const streakResponse = await fetch(
        `${BACKEND_URL}/api/daily-focus/me/streak`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (streakResponse.ok) {
        const data = await streakResponse.json();
        setStreak(data);
      } else {
        console.error('Streak fetch failed:', streakResponse.status);
      } 

      const badgesResponse = await fetch(
        `${BACKEND_URL}/api/badges/me`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (badgesResponse.ok) {
        const data = await badgesResponse.json();
        setBadges(data);
      } else {
        console.error('Badges fetch failed:', badgesResponse.status);
      }
      
    } catch (err) {
      console.error('Fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getBadgeIcon = (BadgeType) => {
    switch(BadgeType) {
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
  
  const getBadgeName = (BadgeType) => {
    switch(BadgeType) {
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
      {/* Streak Display */}
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

      {/* Separator */}
      <div className={styles.divider}></div>      

      {/* Badges Display */}
      {badges.length > 0 ? (
        <div>
          <h3 className={styles.title}>Your Achievements</h3>
          <div className={styles.badgesGrid}>
            {badges.map((badge) => (
              <div key={badge.id} className={styles.badgeItem}>
                <span className={styles.BadgeIcon}>{getBadgeIcon(badge.BadgeType)}</span>
                <span className={styles.BadgeName}>{getBadgeName(badge.BadgeType)}</span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className={styles.noBadges}>
          <p>Complete daily check-ins to earn badges!</p>
        </div>
      )}
    </div>
  );
}
