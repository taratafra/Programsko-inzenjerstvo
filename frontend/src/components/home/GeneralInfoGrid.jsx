import styles from "../../pages/Home/Home.module.css";

export default function GeneralInfoGrid() {
  return (
    <div className={styles.infoGrid}>
      {/* Kartica 1 */}
      <div className={styles.card}>
        <h4>Daily Insights</h4>
        <p style={{ fontSize: "12px", color: "#999" }}>Deep Sleep: 76%</p>
        <div className={styles.chartPlaceholderCircle}><img src={"url('https://ppatour.com/athlete/ivan-jakovljevic/') "}/></div>
        <p style={{ color: "var(--text-accent)", textAlign: "center" }}>
          20% Unlocked
        </p>
      </div>

      {/* Kartica 2 */}
      <div className={styles.card}>
        <h4>Emotional Landscape</h4>
        <div
          className={styles.chartPlaceholderBar}
          style={{
            height: "100px",
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "center",
          }}
        >
          <button className={styles.upgradeButton} style={{ padding: '5px 10px', marginBottom: '5px' }}>
            Weekly Mood Report
          </button>
        </div>
      </div>

      {/* Kartica 3 */}
      <div className={`${styles.card} ${styles.cardWellnessSummary}`}>
        <h4>Wellness Summary</h4>
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            flexDirection: "column",
            margin: "15px 0",
          }}
        >
          <span className={styles.wellnessScore}>3.95</span>
          <p>Average wellness score</p>
        </div>
      </div>

      {/* Kartica 4 */}
      <div className={styles.card}>
        <h4>Mood Distribution</h4>
        <p style={{ color: "var(--text-accent)", textAlign: "right" }}>43%</p>
        <div className={styles.chartPlaceholderBar}></div>
      </div>

      {/* Kartica 5 */}
      <div className={`${styles.card} ${styles.cardRelaxation}`}>
        <div className={styles.cardRelaxationContent}>
          <h4 style={{ marginBottom: "10px" }}>Mindful Relaxation</h4>
          <button className={styles.upgradeButton} style={{ padding: '8px 15px' }}>
            Relaxation Therapy &gt;
          </button>
        </div>
      </div>

      {/* Kartica 6 */}
      <div className={styles.card}>
        <h4>Daily Mood Trends</h4>
        <p style={{ color: "var(--text-accent)", textAlign: "right" }}>4.01</p>
        <div
          className={styles.chartPlaceholderBar}
          style={{ height: "100px" }}
        ></div>
        <p
          style={{
            fontSize: "12px",
            color: "#999",
            textAlign: "center",
          }}
        >
          Average mood score
        </p>
      </div>
    </div>
  );
}