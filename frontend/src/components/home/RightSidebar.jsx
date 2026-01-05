// src/components/home/RightSidebar.jsx

import { useState } from 'react';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import styles from "../../Home.module.css";

export default function RightSidebar({ navigate }) {
  const [date, setDate] = useState(new Date());

  /* Funkcija koja se poziva kod promjene datuma */
  const handleDateChange = (newDate) => {
    setDate(newDate);
    console.log("Odabran datum:", newDate);
  };

  return (
    <div className={`${styles.sidebar} ${styles.calendarSidebar}`}>
      <h3 style={{ textAlign: 'center', margin: '0 0 15px 0' }}>Kalendar</h3>
      <Calendar
        onChange={handleDateChange}
        value={date}
        className={styles.reactCalendar}
      />
    </div>
  );
}