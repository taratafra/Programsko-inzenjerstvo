import React, { useState, useEffect } from 'react';
import styles from './MoodHabits.module.css';

export default function Streak({user}) { 
  const [streak, setStreak] = useState({
    currentStreak: 0,
    longestStreak: 0,
    lastCompletedDate: null
  });

  const [badges, setBadges] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStreakAndBadges();
  }, []);


  /*// Privremeno za testiranje (ukloni kad backend bude spreman)
  const fetchStreakAndBadges = async () => {
    setLoading(true);
    // Mock data
    setStreak({
      currentStreak: 5,
      longestStreak: 12,
      lastCompletedDate: '2025-01-15'
    }); 
    setBadges([
      { id: 1, badgeType: 'STREAK_7', awardedAt: '2025-01-10T10:00:00Z' }
    ]);
    setLoading(false);
  };
*/

  const fetchStreakAndBadges = async () => {
    setLoading(true);
    
    try {
      // Fetch streak
      const streakResponse = await fetch('/api/daily-focus/me/streak', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (streakResponse.ok) {
        const streakData = await streakResponse.json();
        setStreak({
          currentStreak: streakData.currentStreak,
          longestStreak: streakData.longestStreak,
          lastCompletedDate: streakData.lastCompletedDate
        });
      }

      // Fetch badges
      const badgesResponse = await fetch('/api/daily-focus/me/badges', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (badgesResponse.ok) {
        const badgesData = await badgesResponse.json();
        setBadges(badgesData);
      }
      
    } catch (err) {
      console.error('Error fetching streak and badges:', err);
    } finally {
      setLoading(false);
    }
  };
  

  

  const refreshData = () => { //za poziv nakon uspjesnog submita
    fetchStreakAndBadges();
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
      {/* Streak Display */}
      <h3 className={styles.title}>Current Streak</h3>
      {streak.currentStreak > 0 && (
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
      )}

      {/* Separator */}
      <div className={styles.divider}></div>      

      {/* Badges Display */}
      {badges.length > 0 ? (
        <div>
          <h3 className={styles.title}>Your Achievements</h3>
          <div className={styles.badgesGrid}>
            {badges.map((badge) => (
              <div key={badge.id} className={styles.badgeItem}>
                <span className={styles.badgeIcon}>{getBadgeIcon(badge.badgeType)}</span>
                <span className={styles.badgeName}>{getBadgeName(badge.badgeType)}</span>
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

{/*Provjeri jel back ima:
  // U DailyFocusController.java (možda već postoji)
@GetMapping("/me/streak")
public ResponseEntity<StreakStatusResponse> getMyStreak(@AuthenticationPrincipal Jwt jwt) {
    User me = userService.getOrCreateUserFromJwt(jwt);
    UserStreak streak = dailyFocusService.getOrCreateStreak(me.getId());
    return ResponseEntity.ok(StreakStatusResponse.from(streak));
}

@GetMapping("/me/badges")
public ResponseEntity<List<BadgeAwardResponse>> getMyBadges(@AuthenticationPrincipal Jwt jwt) {
    User me = userService.getOrCreateUserFromJwt(jwt);
    List<BadgeAward> badges = badgeRepo.findAllByUserIdOrderByAwardedAtDesc(me.getId());
    return ResponseEntity.ok(badges.stream().map(BadgeAwardResponse::from).toList());
}
  */}