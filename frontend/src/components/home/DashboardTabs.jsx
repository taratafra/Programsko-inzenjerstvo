// src/components/home/DashboardTabs.jsx

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

  return (
    <div className={styles.tabsContainer}>
      {tabs.map((tab) => (
        <button
          key={tab}
          className={`${styles.tabButton} ${activeTab === tab ? styles.tabButtonActive : ""
            }`}
          onClick={() => setActiveTab(tab)}
        >
          {tab}
        </button>
      ))}
    </div>
  );
}