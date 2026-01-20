import styles from "./Statistics.module.css";

export default function EmotionsChart({ data }) {
  const emotionEmojis = {
    HAPPY: '😊',
    SAD: '😢',
    ANXIOUS: '😰',
    CALM: '😌',
    ENERGETIC: '⚡',
    TIRED: '😴',
    FRUSTRATED: '😤',
    GRATEFUL: '🙏',
  };

  const emotionLabels = {
    HAPPY: 'Happy',
    SAD: 'Sad',
    ANXIOUS: 'Anxious',
    CALM: 'Calm',
    ENERGETIC: 'Energetic',
    TIRED: 'Tired',
    FRUSTRATED: 'Frustrated',
    GRATEFUL: 'Grateful',
  };

  // Count emotion frequencies
  const emotionCounts = {};
  data.forEach(checkIn => {
    if (checkIn.emotions && Array.isArray(checkIn.emotions)) {
      checkIn.emotions.forEach(emotion => {
        emotionCounts[emotion] = (emotionCounts[emotion] || 0) + 1;
      });
    }
  });

  const sortedEmotions = Object.entries(emotionCounts)
    .sort((a, b) => b[1] - a[1]);

  const maxCount = Math.max(...Object.values(emotionCounts));

  return (
    <div className={styles.chartCard}>
      <h4 className={styles.chartTitle}>Emotions Frequency</h4>
      <div className={styles.emotionsGrid}>
        {sortedEmotions.map(([emotion, count]) => (
          <div key={emotion} className={styles.emotionBar}>
            <div className={styles.emotionInfo}>
              <span className={styles.emotionIcon}>{emotionEmojis[emotion]}</span>
              <span className={styles.emotionName}>{emotionLabels[emotion]}</span>
              <span className={styles.emotionCount}>{count}</span>
            </div>
            <div className={styles.barContainer}>
              <div 
                className={styles.barFill}
                style={{ width: `${(count / maxCount) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}