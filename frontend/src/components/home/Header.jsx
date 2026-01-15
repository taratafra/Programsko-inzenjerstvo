import styles from "../../pages/Home/Home.module.css";
import DailyMessages from "./DailyMessages";

export default function Header({ navigate, user }) {
  return (
    <div className={styles.topBanner}>
      <div className={styles.logoContainer}>
        <h1 className={styles.logo} onClick={() => navigate("/home")}>
          Mindfulness
        </h1>
        <div className={styles.headerQuoteFull}>
          <DailyMessages user={user} />
        </div>
      </div>
    </div>
  );
}
