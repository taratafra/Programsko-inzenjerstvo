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

  // Helper function to get token from either Auth0 or localStorage
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
        setSchedules(data);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error loading schedules:', error);
      setLoading(false);
    }
  };

  const handleDeleteSchedule = async (scheduleId) => {
    if (!window.confirm('Are you sure you want to delete this schedule?')) {
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
        // Reload schedules after successful deletion
        await loadSchedules();
        
        // Check if there are still schedules for this date after reload
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

  const handleEditSchedule = (schedule) => {
    // Store the schedule to edit in localStorage temporarily
    localStorage.setItem('scheduleToEdit', JSON.stringify(schedule));
    // Navigate to Make Appointment tab
    setActiveTab('Make Appointment');
  };

  useEffect(() => {
    loadSchedules();
  }, []);

  // Reload schedules when returning to this tab
  useEffect(() => {
    if (activeTab === 'Personalized recomendations' || activeTab === 'Make Appointment') {
      loadSchedules();
    }
  }, [activeTab]);

  // Expose loadSchedules to parent component
  useEffect(() => {
    if (onSchedulesReload) {
      onSchedulesReload(loadSchedules);
    }
  }, [onSchedulesReload]);

  // Check if a date has any schedules
  const getSchedulesForDate = (date) => {
    if (!date) return [];
    
    const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][date.getDay()];
    
    // Normalize the date to midnight for comparison
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    
    // Get today's date at midnight for comparison
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    return schedules.filter(schedule => {
      // ONCE type - check if date matches
      if (schedule.repeatType === 'ONCE' && schedule.date) {
        const scheduleDate = new Date(schedule.date);
        scheduleDate.setHours(0, 0, 0, 0);
        return scheduleDate.getTime() === compareDate.getTime();
      }
      
      // DAILY type - check if date is today or in the future
      if (schedule.repeatType === 'DAILY') {
        // Don't show on past dates
        if (compareDate.getTime() < today.getTime()) {
          return false;
        }
        
        // Only show if schedule has started
        if (schedule.date) {
          const startDate = new Date(schedule.date);
          startDate.setHours(0, 0, 0, 0);
          
          // Date must be on or after start date
          if (compareDate.getTime() < startDate.getTime()) {
            return false;
          }
        }
        
        // Optionally check end date if your schedule has one
        if (schedule.endDate) {
          const endDate = new Date(schedule.endDate);
          endDate.setHours(0, 0, 0, 0);
          
          // Date must be on or before end date
          if (compareDate.getTime() > endDate.getTime()) {
            return false;
          }
        }
        
        return true;
      }
      
      // WEEKLY type - check if day of week matches AND date is today or future
      if (schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek) {
        // Don't show on past dates
        if (compareDate.getTime() < today.getTime()) {
          return false;
        }
        
        // Check if date is after start date (if exists)
        if (schedule.date) {
          const startDate = new Date(schedule.date);
          startDate.setHours(0, 0, 0, 0);
          
          if (compareDate.getTime() < startDate.getTime()) {
            return false;
          }
        }
        
        // Check if day of week matches
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
        {/* Month/Year Title - Centered */}
        <div className={styles.monthYearTitle}>
          {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
        </div>

        {/* Calendar Navigation */}
        <div className={styles.calendarHeader}>
          <button onClick={previousMonth} className={styles.navButton}>
            ‹
          </button>
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

        {/* Selected Date Details */}
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
                    <button 
                      onClick={() => handleEditSchedule(schedule)}
                      className={styles.editButton}
                      title="Edit schedule"
                    >
                      ✏️ Edit
                    </button>
                    <button 
                      onClick={() => handleDeleteSchedule(schedule.id)}
                      className={styles.deleteButton}
                      title="Delete schedule"
                    >
                      🗑️ Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
            {/* REMOVED: Create New Schedule button */}
          </div>
        )}

        {/* Empty state for selected date */}
        {showDetails && selectedDateSchedules.length === 0 && (
          <div className={styles.calendarNote}>
            <p>No schedules for this day</p>
          </div>
        )}
      </div>
    </div>
  );
}