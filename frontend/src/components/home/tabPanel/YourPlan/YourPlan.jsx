import React, { useEffect, useState } from 'react';
import styles from './Plan.module.css';

export default function Your7dayPlan({ user, getAccessTokenSilently, isAuthenticated }) {
  const BACKEND_URL = process.env.REACT_APP_BACKEND;

  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const getToken = async () => {
    if (isAuthenticated && getAccessTokenSilently) {
      return await getAccessTokenSilently({
        authorizationParams: {
          audience: process.env.REACT_APP_AUTH0_AUDIENCE,
          scope: "openid profile email",
        },
      });
    }
    return localStorage.getItem('token');
  };

  useEffect(() => {
    fetchPlan();
  }, []);

  const fetchPlan = async () => {
    try {
      const token = await getToken();
      if (!token) throw new Error('No auth token');

      const res = await fetch(`${BACKEND_URL}/onboarding/plan/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error('Failed to load practice plan');
      }

      const data = await res.json();
      setPlan(data);
    } catch (err) {
      console.error(err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <p className={styles.loading}>Loading your 7-day plan…</p>;
  }

  if (error) {
    return <p className={styles.error}>{error}</p>;
  }

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Your 7-Day Practice Plan</h1>

      <p className={styles.subtitle}>
        Valid from {plan.validFrom} to {plan.validTo}
      </p>

      <div className={styles.days}>
        {plan.days.map((day, index) => (
          <div key={index} className={styles.dayCard}>
            <h3 className={styles.dayTitle}>
              {day.title}
            </h3>

            <p className={styles.date}>
              {day.date}
            </p>

            <p className={styles.description}>
              {day.description}
            </p>

            {day.estimatedMinutes && (
              <span className={styles.minutes}>
                ⏱ {day.estimatedMinutes} min
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
