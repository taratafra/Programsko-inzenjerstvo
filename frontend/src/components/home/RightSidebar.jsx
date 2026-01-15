import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./RightSidebar.module.css";

export default function RightSidebar({ navigate, setActiveTab, activeTab, onSchedulesReload }) {
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showDetails, setShowDetails] = useState(false);

  const BACKEND_URL = process.env.REACT_APP_BACKEND;
  const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const dayNames = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

  const getToken = async () => {
    try {
      if (isAuthenticated) {
        return await getAccessTokenSilently({
          authorizationParams: {
            audience: `${AUDIENCE}`,
            scope: "openid profile email",
          },
        });
      } else {
        const localToken = localStorage.getItem("token");
        if (!localToken) {
          throw new Error("No authentication token found");
        }
        return localToken;
      }
    } catch (error) {
      console.error("Error getting token:", error);
      throw error;
    }
  };

  const loadSchedules = async () => {
    try {
      const token = await getToken();
      
      const response = await fetch(`${BACKEND_URL}/api/schedules/me`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Loaded schedules:', data); // Debug: check if trainerId is present
        setSchedules(data);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error loading schedules:', error);
      setLoading(false);
    }
  };

  const handleDeleteEntireSchedule = async (scheduleId) => {
    if (!window.confirm('Are you sure you want to delete this entire schedule? This will remove all occurrences.')) {
      return;
    }

    try {
      const token = await getToken();
      
      const response = await fetch(`${BACKEND_URL}/api/schedules/me/${scheduleId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok || response.status === 204) {
        await loadSchedules();
        
        setTimeout(() => {
          const remainingSchedules = getSchedulesForDate(selectedDate);
          if (remainingSchedules.length === 0) {
            setShowDetails(false);
          }
        }, 100);
        
        alert('Schedule deleted successfully!');
      } else {
        const errorText = await response.text();
        console.error('Delete failed:', errorText);
        alert(`Failed to delete schedule: ${errorText}`);
      }
    } catch (error) {
      console.error('Error deleting schedule:', error);
      alert(`Error deleting schedule: ${error.message}`);
    }
  };

  const handleDeleteSpecificDate = async (schedule) => {
    const dateStr = selectedDate.toLocaleDateString();
    
    if (!window.confirm(`Are you sure you want to remove this schedule from ${dateStr} only?`)) {
      return;
    }

    try {
      const token = await getToken();
      
      // Format date as YYYY-MM-DD
      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
      const day = String(selectedDate.getDate()).padStart(2, '0');
      const formattedDate = `${year}-${month}-${day}`;
      
      // Add this date to the excluded dates list
      const existingExcludedDates = schedule.excludedDates || [];
      const updatedExcludedDates = [...existingExcludedDates, formattedDate];
      
      // Make sure trainerId exists - this is required!
      if (!schedule.trainerId) {
        console.error('Schedule missing trainerId:', schedule);
        alert('Error: Schedule is missing trainer information. Please refresh and try again.');
        return;
      }
      
      let updatedSchedule = {
        title: schedule.title,
        startTime: schedule.startTime,
        repeatType: schedule.repeatType,
        trainerId: schedule.trainerId, // Must not be undefined!
        timezone: schedule.timezone || 'Europe/Zagreb',
        reminderMinutesBefore: schedule.reminderMinutesBefore != null ? schedule.reminderMinutesBefore : 30,
        enabled: schedule.enabled != null ? schedule.enabled : true,
        excludedDates: updatedExcludedDates
      };

      // Add required fields based on repeat type
      if (schedule.repeatType === 'WEEKLY') {
        updatedSchedule.daysOfWeek = schedule.daysOfWeek; // Keep all the days
        updatedSchedule.date = null; // WEEKLY must have date as null
      } else if (schedule.repeatType === 'DAILY') {
        updatedSchedule.daysOfWeek = []; // DAILY must have empty daysOfWeek
        updatedSchedule.date = null; // DAILY must have date as null
      } else if (schedule.repeatType === 'ONCE') {
        updatedSchedule.daysOfWeek = []; // ONCE must have empty daysOfWeek
        updatedSchedule.date = schedule.date; // ONCE must have the date
      }

      console.log('Sending update:', updatedSchedule); // Debug log

      const response = await fetch(`${BACKEND_URL}/api/schedules/me/${schedule.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(updatedSchedule)
      });

      if (response.ok) {
        await loadSchedules();
        
        setTimeout(() => {
          const remainingSchedules = getSchedulesForDate(selectedDate);
          if (remainingSchedules.length === 0) {
            setShowDetails(false);
          }
        }, 100);
        
        alert('Schedule updated successfully!');
      } else {
        const errorText = await response.text();
        console.error('Update failed - Status:', response.status);
        console.error('Update failed - Response:', errorText);
        
        try {
          const errorJson = JSON.parse(errorText);
          alert(`Failed to update schedule: ${errorJson.message || JSON.stringify(errorJson, null, 2)}`);
        } catch (e) {
          alert(`Failed to update schedule: ${errorText}`);
        }
      }
    } catch (error) {
      console.error('Error updating schedule:', error);
      alert(`Error updating schedule: ${error.message}`);
    }
  };

  useEffect(() => {
    loadSchedules();
  }, []);

  useEffect(() => {
    if (activeTab === 'Personalized recomendations' || activeTab === 'Make Appointment') {
      loadSchedules();
    }
  }, [activeTab]);

  useEffect(() => {
    if (onSchedulesReload) {
      onSchedulesReload(loadSchedules);
    }
  }, []);

  const getSchedulesForDate = (date) => {
    if (!date) return [];
    
    const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][date.getDay()];
    
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Format date as YYYY-MM-DD for comparison with excludedDates
    const year = compareDate.getFullYear();
    const month = String(compareDate.getMonth() + 1).padStart(2, '0');
    const day = String(compareDate.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;
    
    return schedules.filter(schedule => {
      // Check if this date is in the excluded dates list
      if (schedule.excludedDates && schedule.excludedDates.includes(formattedDate)) {
        return false;
      }
      
      if (schedule.repeatType === 'ONCE' && schedule.date) {
        const scheduleDate = new Date(schedule.date);
        scheduleDate.setHours(0, 0, 0, 0);
        return scheduleDate.getTime() === compareDate.getTime();
      }
      
      if (schedule.repeatType === 'DAILY') {
        if (compareDate.getTime() < today.getTime()) {
          return false;
        }
        
        if (schedule.date) {
          const startDate = new Date(schedule.date);
          startDate.setHours(0, 0, 0, 0);
          
          if (compareDate.getTime() < startDate.getTime()) {
            return false;
          }
        }
        
        if (schedule.endDate) {
          const endDate = new Date(schedule.endDate);
          endDate.setHours(0, 0, 0, 0);
          
          if (compareDate.getTime() > endDate.getTime()) {
            return false;
          }
        }
        
        return true;
      }
      
      if (schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek) {
        if (compareDate.getTime() < today.getTime()) {
          return false;
        }
        
        if (schedule.date) {
          const startDate = new Date(schedule.date);
          startDate.setHours(0, 0, 0, 0);
          
          if (compareDate.getTime() < startDate.getTime()) {
            return false;
          }
        }
        
        return schedule.daysOfWeek.includes(dayOfWeek);
      }
      
      return false;
    });
  };

  const hasSchedule = (date) => {
    return getSchedulesForDate(date).length > 0;
  };

  const getDaysInMonth = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    
    let startingDayOfWeek = firstDay.getDay();
    startingDayOfWeek = startingDayOfWeek === 0 ? 6 : startingDayOfWeek - 1;
    
    const days = [];
    
    for (let i = 0; i < startingDayOfWeek; i++) {
      days.push(null);
    }
    
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day));
    }
    
    return days;
  };

  const handleDateClick = (date) => {
    if (!date) return;
    
    setSelectedDate(date);
    const dateSchedules = getSchedulesForDate(date);
    
    if (dateSchedules.length > 0) {
      setShowDetails(true);
    } else {
      setShowDetails(false);
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
  const selectedDateSchedules = getSchedulesForDate(selectedDate);

  return (
    <div className={styles.calendarSidebar}>
      <h3 className={styles.calendarTitle}>Calendar</h3>
      
      <div className={styles.calendar}>
        <div className={styles.monthYearTitle}>
          {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
        </div>

        <div className={styles.calendarHeader}>
          <button onClick={previousMonth} className={styles.navButton}>
            ‹
          </button>
          <button onClick={nextMonth} className={styles.navButton}>
            ›
          </button>
        </div>

        <div className={styles.dayNames}>
          {dayNames.map(day => (
            <div key={day} className={styles.dayName}>{day}</div>
          ))}
        </div>

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
                ${date && hasSchedule(date) ? styles.hasAppointment : ''}
              `}
            >
              {date && (
                <>
                  <span className={styles.dayNumber}>{date.getDate()}</span>
                  {hasSchedule(date) && (
                    <div className={styles.appointmentDots}>
                      {getSchedulesForDate(date).map((schedule, idx) => (
                        <span 
                          key={idx}
                          className={`${styles.appointmentDot} ${
                            schedule.repeatType === 'ONCE' ? styles.dotOnce :
                            schedule.repeatType === 'DAILY' ? styles.dotDaily :
                            styles.dotWeekly
                          }`}
                        ></span>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          ))}
        </div>

        {showDetails && selectedDateSchedules.length > 0 && (
          <div className={styles.scheduleDetails}>
            <div className={styles.detailsHeader}>
              <h4>📅 {selectedDate.toLocaleDateString()}</h4>
              <button 
                onClick={() => setShowDetails(false)} 
                className={styles.closeButton}
              >
                ×
              </button>
            </div>
            
            <div className={styles.schedulesList}>
              {selectedDateSchedules.map((schedule, index) => (
                <div key={schedule.id || index} className={styles.scheduleItem}>
                  <div className={styles.scheduleTime}>
                    🕐 {schedule.startTime}
                  </div>
                  <div className={styles.scheduleTitle}>
                    {schedule.title}
                  </div>
                  <div className={styles.scheduleType}>
                    {schedule.repeatType === 'ONCE' ? '📅 One-time' : 
                     schedule.repeatType === 'DAILY' ? '🔄 Daily' : 
                     '📆 Weekly'}
                  </div>
                  {schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek && (
                    <div className={styles.scheduleDays}>
                      {schedule.daysOfWeek.map(day => day.substring(0, 3)).join(', ')}
                    </div>
                  )}
                  
                  <div className={styles.scheduleActions}>
                    {/* For ONCE - only show delete entire */}
                    {schedule.repeatType === 'ONCE' && (
                      <button 
                        onClick={() => handleDeleteEntireSchedule(schedule.id)}
                        className={styles.deleteButton}
                        title="Delete this schedule"
                      >
                        🗑️ Delete
                      </button>
                    )}
                    
                    {/* For WEEKLY - show both options */}
                    {schedule.repeatType === 'WEEKLY' && (
                      <>
                        <button 
                          onClick={() => handleDeleteSpecificDate(schedule)}
                          className={styles.deleteThisDateButton}
                          title="Remove this day from weekly schedule"
                        >
                          🗑️ Remove This Day
                        </button>
                        <button 
                          onClick={() => handleDeleteEntireSchedule(schedule.id)}
                          className={styles.deleteButton}
                          title="Delete entire weekly schedule"
                        >
                          🗑️ Delete All
                        </button>
                      </>
                    )}
                    
                    {/* For DAILY - show both options */}
                    {schedule.repeatType === 'DAILY' && (
                      <>
                        <button 
                          onClick={() => handleDeleteSpecificDate(schedule)}
                          className={styles.deleteThisDateButton}
                          title="Remove this specific date"
                        >
                          🗑️ Remove This Date
                        </button>
                        <button 
                          onClick={() => handleDeleteEntireSchedule(schedule.id)}
                          className={styles.deleteButton}
                          title="Delete entire daily schedule"
                        >
                          🗑️ Delete All
                        </button>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {showDetails && selectedDateSchedules.length === 0 && (
          <div className={styles.calendarNote}>
            <p>No schedules for this day</p>
          </div>
        )}
      </div>
    </div>
  );
}