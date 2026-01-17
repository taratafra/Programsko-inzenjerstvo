import { useState } from "react";
import styles from "./Statistics.module.css";

export default function TextualDataDisplay({ data }) {
  const [expandedIndex, setExpandedIndex] = useState(null);

  // Sort by date (newest first)
  const sortedData = [...data]
    .filter(checkIn => 
      checkIn.caffeineIntake || 
      checkIn.alcoholIntake || 
      checkIn.physicalActivity || 
      checkIn.notes
    )
    .sort((a, b) => new Date(b.date) - new Date(a.date));

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric',
      year: 'numeric'
    });
  };

  return (
    <div className={styles.chartCard}>
      <h4 className={styles.chartTitle}>Daily Entries</h4>
      <div className={styles.entriesContainer}>
        {sortedData.map((checkIn, index) => (
          <div key={checkIn.date} className={styles.entryCard}>
            <div 
              className={styles.entryHeader}
              onClick={() => setExpandedIndex(expandedIndex === index ? null : index)}
            >
              <span className={styles.entryDate}>{formatDate(checkIn.date)}</span>
              <span className={styles.expandIcon}>
                {expandedIndex === index ? '▼' : '▶'}
              </span>
            </div>
            
            {expandedIndex === index && (
              <div className={styles.entryContent}>
                {checkIn.caffeineIntake && (
                  <div className={styles.entryItem}>
                    <span className={styles.entryLabel}>☕ Caffeine:</span>
                    <span className={styles.entryValue}>{checkIn.caffeineIntake}</span>
                  </div>
                )}
                {checkIn.alcoholIntake && (
                  <div className={styles.entryItem}>
                    <span className={styles.entryLabel}>🍷 Alcohol:</span>
                    <span className={styles.entryValue}>{checkIn.alcoholIntake}</span>
                  </div>
                )}
                {checkIn.physicalActivity && (
                  <div className={styles.entryItem}>
                    <span className={styles.entryLabel}>🏃 Activity:</span>
                    <span className={styles.entryValue}>{checkIn.physicalActivity}</span>
                  </div>
                )}
                {checkIn.notes && (
                  <div className={styles.entryItem}>
                    <span className={styles.entryLabel}>📝 Notes:</span>
                    <span className={styles.entryValue}>{checkIn.notes}</span>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}