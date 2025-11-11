import styles from "../../Home.module.css";

export default function Header({ navigate, user }) {
  return (
    <div className={styles.topBanner}>
      <div className={styles.logoContainer}>
        <h1 className={styles.logo} onClick={() => navigate("/home")}>
          Modly
        </h1>
        <p className={styles.headerQuote}>
          "Ovdje će se uskoro pojaviti motivacijski citati..."
        </p>
      </div>

      <div className={styles.headerActions}>
        <input
          type="text"
          placeholder="🔍 Search something..."
          className={styles.searchInput}
        />
        <button className={styles.upgradeButton}>Upgrade</button>
      </div>
    </div>
  );
}