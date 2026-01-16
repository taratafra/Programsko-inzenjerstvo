import homeStyles from "../../pages/Home/Home.module.css";
import { useNavigate, useLocation } from "react-router-dom";
import styles from "./LeftSidebar.module.css";

export default function LeftSidebar({ user, handleLogout, activeTab, setActiveTab }) {
  const navigate = useNavigate();

  const getNavItemClass = (tabName) => {
    const isActive = (tabName === 'Home' && activeTab === 'General Information') || activeTab === tabName;
    return `${homeStyles.navItem} ${isActive ? homeStyles.navItemActive : ""}`;
  };

  return (
    <div className={homeStyles.sidebar}>
      <div className={homeStyles.profileBox}>
        <p className={homeStyles.profileName}>{user.name} {user.surname}</p>
        <p className={homeStyles.profileTitle}>{user.email}</p>
      </div>
      <ul className={homeStyles.navList}>
        <li className={getNavItemClass("Home")} onClick={() => setActiveTab('General Information')}>
          🏠 Home
        </li>
        <li className={getNavItemClass("Trainers")} onClick={() => setActiveTab('Trainers')}>
          👥 Trainers
        </li>
        <li className={getNavItemClass("Videos")} onClick={() => setActiveTab('Videos')}>
          🎥 Videos
        </li>
        <li className={getNavItemClass("Articles")} onClick={() => setActiveTab('Articles')}>
          📄 Articles
        </li>
        <li className={getNavItemClass("Podcasts")} onClick={() => setActiveTab('Podcasts')}>
          🎙️ Podcasts
        </li>
        <li className={getNavItemClass("Make Appointment")} onClick={() => setActiveTab('Make Appointment')}>
          📅 Make Appointment
        </li>
        <li className={getNavItemClass("Calendar")} onClick={() => setActiveTab('Calendar')}>
          🗓️ Calendar
        </li>
        <li className={getNavItemClass("DailyFocus")} onClick={() => setActiveTab('DailyFocus')}>
          <span className={homeStyles.navItemLogout}>📒 Daily Focus</span>
        </li>
        <li className={`${getNavItemClass("Statistics")} ${styles.statisticsItem}`} onClick={() => setActiveTab('Statistics')}>
          📈 Statistics
        </li>
        <li className={getNavItemClass("Settings")} onClick={() => setActiveTab('Settings')}>
          <span className={homeStyles.navItemLogout}>⚙️ Settings</span>
        </li>
        <li className={homeStyles.navItem} onClick={handleLogout}>
          ➡️ Log Out
        </li>
      </ul>
    </div>
  );
}