import styles from "../../Home.module.css";
export default function LeftSidebar({ user, handleLogout, activeTab, setActiveTab }) {
  const getNavItemClass = (tabName) => {
    const isActive = (tabName === 'Home' && activeTab === 'General Information') || activeTab === tabName;
    return `${styles.navItem} ${isActive ? styles.navItemActive : ""}`;
  };

  const getAvatarClass = () => {
    return `${styles.profileAvatar} ${activeTab === 'Account' ? styles.profileAvatarActive : ""}`;
  };

  return (
    <div className={styles.sidebar}>
      <div className={styles.profileBox}>
        <img src={user.picture} alt="Profile" className={getAvatarClass()}
        onClick={() => setActiveTab('Account')} />
        <p className={styles.profileName}>{ user.name }</p>
        <p className={styles.profileTitle}>{user.email}</p> 
      </div>
      
      <ul className={styles.navList}>
        <li className={getNavItemClass("Home")} onClick={() => setActiveTab('General Information')}>
          🏠 Home
        </li>
        <li className={getNavItemClass("Calendar")} onClick={() => setActiveTab('Calendar')}>
          📅 Calendar
        </li>
        <li className={getNavItemClass("Journal")} onClick={() => setActiveTab('Journal')}>
          📒 Journal
        </li>
        <li className={getNavItemClass("Statistics")} onClick={() => setActiveTab('Statistics')} style={{ marginBottom: "80px" }}>
          📈 Statistics
        </li>
        <li className={getNavItemClass("Settings")} onClick={() => setActiveTab('Settings')}>
          <span className={styles.navItemLogout}>⚙️ Settings</span>
        </li>
        <li className={styles.navItem} onClick={handleLogout}>
          ➡️ Log Out
        </li>
      </ul>
    </div>
  );
}