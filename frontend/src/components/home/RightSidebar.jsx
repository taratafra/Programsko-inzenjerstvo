// src/components/home/RightSidebar.jsx

import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./RightSidebar.module.css";

export default function RightSidebar({ navigate, setActiveTab }) {
  const { getAccessTokenSilently } = useAuth0();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);

  const BACKEND_URL = process.env.REACT_APP_BACKEND;

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const dayNames = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

  useEffect(() => {
    loadAppointments();
  }, []);

  const loadAppointments = async () => {
    try {
      const token = await getAccessTokenSilently();
      
      const response = await fetch(`${BACKEND_URL}/api/schedules/me`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setAppointments(data);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error loading appointments:', error);
      setLoading(false);
    }
  };

  const hasAppointment = (date) => {
    return appointments.some(apt => {
      const aptDate = new Date(apt.startTime);
      return aptDate.toDateString() === date.toDateString();
    });
  };

  const getDaysInMonth = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    
    // Get the day of week (0 = Sunday, 1 = Monday, etc.)
    let startingDayOfWeek = firstDay.getDay();
    // Convert to Monday = 0, Sunday = 6
    startingDayOfWeek = startingDayOfWeek === 0 ? 6 : startingDayOfWeek - 1;
    
    const days = [];
    
    // Add empty slots for days before the month starts
    for (let i = 0; i < startingDayOfWeek; i++) {
      days.push(null);
    }
    
    // Add all days of the month
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day));
    }
    
    return days;
  };

  const handleDateClick = (date) => {
    if (!date) return;
    
    setSelectedDate(date);
    console.log('Date clicked:', date);
    console.log('Has appointment:', hasAppointment(date));
    console.log('setActiveTab function exists:', !!setActiveTab);
    
    if (hasAppointment(date) && setActiveTab) {
      console.log('Navigating to Make Appointment tab');
      setActiveTab('Make Appointment');
    }
  };

  const previousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
  };

  const isToday = (date) => {
    if (!date) return false;
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  const isSelected = (date) => {
    if (!date) return false;
    return date.toDateString() === selectedDate.toDateString();
  };

  const days = getDaysInMonth(currentDate);

  return (
    <div className={styles.calendarSidebar}>
      <h3 className={styles.calendarTitle}>Kalendar</h3>
      
      <div className={styles.calendar}>
        {/* Calendar Header */}
        <div className={styles.calendarHeader}>
          <button onClick={previousMonth} className={styles.navButton}>
            ‹
          </button>
          <span className={styles.monthYear}>
            {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
          </span>
          <button onClick={nextMonth} className={styles.navButton}>
            ›
          </button>
        </div>

        {/* Day Names */}
        <div className={styles.dayNames}>
          {dayNames.map(day => (
            <div key={day} className={styles.dayName}>{day}</div>
          ))}
        </div>

        {/* Calendar Days */}
        <div className={styles.daysGrid}>
          {days.map((date, index) => (
            <div
              key={index}
              onClick={() => handleDateClick(date)}
              className={`
                ${styles.dayCell}
                ${!date ? styles.emptyCell : ''}
                ${isToday(date) ? styles.today : ''}
                ${isSelected(date) ? styles.selected : ''}
                ${date && hasAppointment(date) ? styles.hasAppointment : ''}
              `}
            >
              {date && (
                <>
                  <span className={styles.dayNumber}>{date.getDate()}</span>
                  {hasAppointment(date) && (
                    <span className={styles.appointmentDot}></span>
                  )}
                </>
              )}
            </div>
          ))}
        </div>

        {/* Selected Date Info */}
        {selectedDate && hasAppointment(selectedDate) && (
          <div className={styles.calendarNote}>
            <p>📅 Appointment scheduled</p>
          </div>
        )}
      </div>
    </div>
  );
}