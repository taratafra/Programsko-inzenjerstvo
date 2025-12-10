import homeStyles from "../../pages/Home/Home.module.css";
import styles from "./LeftSidebar.module.css";

export default function LeftSidebar({ user, handleLogout, activeTab, setActiveTab }) {

  const getNavItemClass = (tabName) => {
    const isActive = (tabName === 'Home' && activeTab === 'General Information') || activeTab === tabName;
    return `${homeStyles.navItem} ${isActive ? homeStyles.navItemActive : ""}`;
  };

  const getAvatarClass = () => {
    return `${homeStyles.profileAvatar} ${activeTab === 'Account' ? homeStyles.profileAvatarActive : ""}`;
  };

  return (
    <div className={homeStyles.sidebar}>
      <div className={homeStyles.profileBox}>
        <img src={user.picture} alt="Profile" className={getAvatarClass()}
          onClick={() => setActiveTab('Account')} />
        <p className={homeStyles.profileName}>{user.name}</p>
        <p className={homeStyles.profileTitle}>{user.email}</p>
      </div>

      <ul className={homeStyles.navList}>
        <li className={getNavItemClass("Home")} onClick={() => setActiveTab('General Information')}>
          🏠 Home
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
        <li classNameG={getNavItemClass("Settings")} onClick={() => setActiveTab('Settings')}>
          <span className={homeStyles.navItemLogout}>⚙️ Settings</span>
        </li>
        <li className={homeStyles.navItem} onClick={handleLogout}>
          ➡️ Log Out
        </li>
      </ul>
    </div>
  );
}