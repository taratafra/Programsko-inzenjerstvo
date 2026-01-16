import HomeStyles from "../../../../pages/Home/Home.module.css"; // koristi isti CSS kao tabPanel
import styles from "../../../../pages/Home/Home.module.css";

import { useState, useEffect} from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

import Exercise from "./Exercise";
import MoodHabits from "./MoodHabits";
import Streak from "./Streak";
import Meditation from "./Meditation";


export default function DailyFocus({user, activeTab, setActiveTab}) {  
  const [activeSubTab, setActiveSubTab] = useState("Meditation");
  const Navigate=useNavigate();
  const { isAuthenticated } = useAuth0();

  const tabs = [
    "Meditation",
    "Excercise",
    "Mood & Habits",
    "Streak"
  ];

  const renderSubTabContent = () => {
      switch (activeSubTab) {
        case "Meditation":
          return <Meditation user={user}/>

        case "Excercise":
          return <Exercise user={user}/>
        
        case "Mood & Habits":
          return <MoodHabits user={user}/>  
        
        case "Streak":
          return <Streak user={user}/>
        
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

{/* Goal badge */}
{/*<div className={styles.goalBadgeContainer}>
    <span className={styles.goalBadge}>
        {getGoalLabel(item.goal)}
    </span>
</div>*/}