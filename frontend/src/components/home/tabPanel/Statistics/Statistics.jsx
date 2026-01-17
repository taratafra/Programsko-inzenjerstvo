import {useEffect, useState } from "react";
import MetricChart from "./MetricChart";
import styles from "./Statistics.module.css";

//`${BACKEND_URL}/api/mood-checkins/me`
export default function Statistics({getAccessTokenSilently}) {
    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const [checkIns, setCheckIns] = useState([]);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const token = await getAccessTokenSilently();

        const res = await fetch(`${BACKEND_URL}/api/mood-checkins/me`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        const data = await res.json();

        if (Array.isArray(data)) {
            setCheckIns(data);
            } else {
            console.error("Expected array from backend, got:", data);
            setCheckIns([]); // fallback
            }
      } catch (error) {
        console.error("Failed to fetch statistics:", error);
        setCheckIns([]); // fallback
      }
    };

    fetchStats();
  }, [BACKEND_URL, getAccessTokenSilently]);

  //provjeri ima li podataka za specificnu metriku
  const hasDataForMetric = (dataKey) => {
    return checkIns.some(checkIn => checkIn[dataKey] != null);
  };

  return (
    <div className={styles.statisticsContainer}>
      <h2 className={styles.title}>Your Wellbeing Statistics</h2>
{checkIns.length === 0 ? (
      <p className={styles.noData}>
        <strong>No data available yet.</strong>
        <br />
        Start tracking your moods and habits to see your progress here.
      </p>
    ) : (
      <>
        <MetricChart title="Mood" data={checkIns} dataKey="moodScore" />
        <MetricChart title="Sleep Quality" data={checkIns} dataKey="sleepQuality" />
        <MetricChart title="Stress Level" data={checkIns} dataKey="stressLevel" />
        <MetricChart title="Focus Level" data={checkIns} dataKey="focusLevel" />
      </>
    )}
    </div>
  );
} 

