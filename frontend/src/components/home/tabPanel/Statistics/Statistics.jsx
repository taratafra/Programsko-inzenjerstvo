import {useEffect, useState } from "react";
import MetricChart from "./MetricChart";
import EmotionsChart from "./EmotionsChart";
import TextualDataDisplay from "./TextualDataDisplay";
import styles from "./Statistics.module.css";

export default function Statistics({getAccessTokenSilently, isAuthenticated, refreshTrigger}) {
    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const [checkIns, setCheckIns] = useState([]);

  const getToken = async () => {
    try {
      // Auth0 flow - samo ako je authenticated
      if (isAuthenticated && getAccessTokenSilently) {
        try {
          const token = await getAccessTokenSilently({
            authorizationParams: {
              audience: process.env.REACT_APP_AUTH0_AUDIENCE,
            },
          });
          return token;
        } catch (authError) {
          //console.warn("Auth0 failed, trying localStorage:", authError.message);
        }
      }

      // Fallback na localStorage
      const localToken = localStorage.getItem('token');
      if (localToken) {
        //console.log("Got token from localStorage");
        return localToken;
      }

      //console.error("No token found anywhere!");
      return null;
    } catch (err) {
      console.error('Error getting token:', err);
      return null;
    }
  };

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const token = await getToken();
        
        if (!token) {
          return;
        }

        const res = await fetch(`${BACKEND_URL}/api/mood-checkins/me`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }

        const data = await res.json();

        if (Array.isArray(data)) {
            setCheckIns(data);
        } else {
            setCheckIns([]);
        }
      } catch (error) {
        setCheckIns([]);
      }
    };

    fetchStats();
  }, [BACKEND_URL, refreshTrigger, isAuthenticated]);

  const hasDataForMetric = (dataKey) => {
    return checkIns.some(checkIn => checkIn[dataKey] != null);
  };

  const hasEmotionsData = () => {
    return checkIns.some(checkIn => checkIn.emotions && checkIn.emotions.length > 0);
  };

  const hasTextualData = () => {
    return checkIns.some(checkIn => 
      checkIn.caffeineIntake || 
      checkIn.alcoholIntake || 
      checkIn.physicalActivity || 
      checkIn.notes
    );
  };

  return (
    <div className={styles.statisticsContainer}>
      <h2 className={styles.title}>Your Mood & Habits Statistics</h2>
        {checkIns.length === 0 ? (
        <p className={styles.noData}>
          <strong>No data available yet.</strong>
          <br />
          Start tracking your moods and habits to see your progress here.
        </p>
      ) : (
        <>
          {/* Numeric metrics charts */}
          {hasDataForMetric('moodScore') && (
            <MetricChart title="Mood" data={checkIns} dataKey="moodScore" />
          )}
          {hasDataForMetric('sleepQuality') && (
            <MetricChart title="Sleep Quality" data={checkIns} dataKey="sleepQuality" />
          )}
          {hasDataForMetric('stressLevel') && (
            <MetricChart title="Stress Level" data={checkIns} dataKey="stressLevel" />
          )}
          {hasDataForMetric('focusLevel') && (
            <MetricChart title="Focus Level" data={checkIns} dataKey="focusLevel" />
          )}

          {/* Emotions visualization */}
          {hasEmotionsData() && (
            <EmotionsChart data={checkIns} />
          )}

          {/* Textual data display */}
          {hasTextualData() && (
            <TextualDataDisplay data={checkIns} />
          )}
        </>
      )}
    </div>
  );
}