import styles from "../../../../pages/Home/Home.module.css";

import { useState} from "react";

import Exercise from "./Exercise";
import Streak from "./Streak";
import Meditate from "./Meditation";


export default function DailyFocus({ user, getAccessTokenSilently, isAuthenticated }) {  
  const [activeSubTab, setActiveSubTab] = useState("Meditate");

  const tabs = [
    "Meditate",
    "Excercise",
    "Reflect"
  ];

  const renderSubTabContent = () => {
      switch (activeSubTab) {
        case "Meditate":
          return <Meditate user={user}/>

        case "Excercise":
          return <Exercise user={user}/>
        
        case "Reflect":
          return <Streak 
            user={user}
            getAccessTokenSilently={getAccessTokenSilently}
            isAuthenticated={isAuthenticated}
            />
        
        default:
          return null;
      }
    };

    return (
      <div>
        {/* Tabovi za podsekcije */}
        <div className={styles.tabsContainer}>
          {tabs.map((tab) => (
            <button
              key={tab}
              className={`${styles.tabButton} ${
                activeSubTab === tab ? styles.tabButtonActive : ""
              }`}
              onClick={() => setActiveSubTab(tab)}
            >
              {tab}
            </button>
          ))}
        </div>
        
        {/* Sadržaj aktivnog podtaba */}
        <div className={styles.tabPanel}>
          {renderSubTabContent()}
        </div>
      </div>
    );
}
