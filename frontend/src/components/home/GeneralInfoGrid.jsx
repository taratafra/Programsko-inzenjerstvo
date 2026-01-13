import homeStyles from "../../pages/Home/Home.module.css";
import styles from "./GeneralInfoGrid.module.css";

export default function GeneralInfoGrid() {
  return (
    <div className={homeStyles.infoGrid}>
      {/* Kartica 1 */}
      <div className={homeStyles.card}>
        <h4>Daily Insights</h4>
        <p className={styles.deepSleepText}>Deep Sleep: 76%</p>
        <div className={homeStyles.chartPlaceholderCircle}><img src={"url('https://ppatour.com/athlete/ivan-jakovljevic/') "} /></div>
        <p className={styles.unlockedText}>
          20% Unlocked
        </p>
      </div>

      {/* Kartica 2 */}
      <div className={homeStyles.card}>
        <h4>Emotional Landscape</h4>
        <div className={`${homeStyles.chartPlaceholderBar} ${styles.barChartContainer}`}>
          <button className={`${homeStyles.upgradeButton} ${styles.weeklyMoodButton}`}>
            Weekly Mood Report
          </button>
        </div>
      </div>

      {/* Kartica 3 */}
      <div className={`${homeStyles.card} ${homeStyles.cardWellnessSummary}`}>
        <h4>Wellness Summary</h4>
        <div className={styles.wellnessSummaryContent}>
          <span className={homeStyles.wellnessScore}>3.95</span>
          <p>Average wellness score</p>
        </div>
      </div>

      {/* Kartica 4 */}
      <div className={homeStyles.card}>
        <h4>Mood Distribution</h4>
        <p className={styles.moodPercentage}>43%</p>
        <div className={homeStyles.chartPlaceholderBar}></div>
      </div>

      {/* Kartica 5 */}
      <div className={`${homeStyles.card} ${homeStyles.cardRelaxation}`}>
        <div className={homeStyles.cardRelaxationContent}>
          <h4 className={styles.relaxationTitle}>Mindful Relaxation</h4>
          <button className={`${homeStyles.upgradeButton} ${styles.relaxationButton}`}>
            Relaxation Therapy &gt;
          </button>
        </div>
      </div>

      {/* Kartica 6 */}
      <div className={homeStyles.card}>
        <h4>Daily Mood Trends</h4>
        <p className={styles.moodPercentage}>4.01</p>
        <div className={`${homeStyles.chartPlaceholderBar} ${styles.trendChartContainer}`}></div>
        <p className={styles.averageMoodText}>
          Average mood score
        </p>
      </div>
    </div>
  );
}