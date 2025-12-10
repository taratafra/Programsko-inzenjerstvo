// src/components/home/RightSidebar.jsx

import { useState } from 'react';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import homeStyles from "../../pages/Home/Home.module.css";
import styles from "./RightSidebar.module.css";

export default function RightSidebar({ navigate }) {
  const [date, setDate] = useState(new Date());

  /* Funkcija koja se poziva kod promjene datuma */
  const handleDateChange = (newDate) => {
    setDate(newDate);
    console.log("Odabran datum:", newDate);
  };

  return (
    <div className={`${homeStyles.sidebar} ${homeStyles.calendarSidebar}`}>
      <h3 className={styles.calendarTitle}>Kalendar</h3>
      <Calendar
        onChange={handleDateChange}
        value={date}
        className={homeStyles.reactCalendar}
      />
    </div>
  );
}