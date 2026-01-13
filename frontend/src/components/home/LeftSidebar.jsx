import homeStyles from "../../pages/Home/Home.module.css";
import { useNavigate, useLocation } from "react-router-dom";
import styles from "./LeftSidebar.module.css";

export default function LeftSidebar({ user, handleLogout, activeTab, setActiveTab }) {
  const navigate = useNavigate();
  const location = useLocation();

  const getNavItemClass = (tabName) => {
    const isActive = (tabName === 'Home' && activeTab === 'General Information') || activeTab === tabName;
    return `${homeStyles.navItem} ${isActive ? homeStyles.navItemActive : ""}`;
  };

  const getAvatarClass = () => {
    return `${homeStyles.profileAvatar} ${activeTab === 'Account' ? homeStyles.profileAvatarActive : ""}`;
  };

  const handleHomeClick = () => {
    if (location.pathname === '/home') {
      setActiveTab('General Information');
    } else {
      navigate('/home');
    }
  };

  return (
    <div className={homeStyles.sidebar}>
      <div className={homeStyles.profileBox}>
        <p className={homeStyles.profileName}>{user.name}</p>
        <p className={homeStyles.profileTitle}>{user.email}</p>
      </div>

      <ul className={homeStyles.navList}>
        <li className={getNavItemClass("Home")} onClick={handleHomeClick}>
          🏠 Home
        </li>
        <li className={getNavItemClass("Videos")} onClick={() => navigate('/videos')}>
          🎥 Videos
        </li>
        <li className={getNavItemClass("Calendar")} onClick={() => setActiveTab('Calendar')}>
          📅 Calendar
        </li>
        <li className={getNavItemClass("Journal")} onClick={() => setActiveTab('Journal')}>
          📒 Journal
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
