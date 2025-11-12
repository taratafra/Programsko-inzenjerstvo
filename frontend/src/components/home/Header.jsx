import styles from "../../Home.module.css";
import DailyMessages from "./DailyMessages";

export default function Header({ navigate, user }) {
  return (
    <div className={styles.topBanner}>
      <div className={styles.logoContainer}>
        <h1 className={styles.logo} onClick={() => navigate("/home")}>
          Modly
        </h1>
        <div className={styles.headerQuote}>
          <DailyMessages user={user} />
        </div>
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