
import styles from "../../pages/Home/Home.module.css";

export default function DashboardTabs({ activeTab, setActiveTab }) {
  
  const tabs = [
    "Personalized recomendations",
    "Focus",
    "Sleep",
    "Stress",
    "Gratitude",
    "Breathing",
  ];

  const homeTabs = ["Personalized recomendations", "Focus", "Sleep", "Stress", "Gratitude", "Breathing", "General Information"];

  if (!homeTabs.includes(activeTab)) {
    return null;
  }

  return (
    <div className={styles.tabsContainer}>
      {tabs.map((tab) => (
        <button
          key={tab}
          className={`${styles.tabButton} ${
            activeTab === tab ? styles.tabButtonActive : ""
          }`}
          onClick={() => setActiveTab(tab)}
        >
          {tab}
        </button>
      ))}
    </div>
  );
}