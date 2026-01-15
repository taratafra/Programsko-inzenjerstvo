import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from './MakeAppointment.module.css';

export default function MakeAppointment({ setActiveTab, reloadCalendar }) {
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const [subscribedTrainers, setSubscribedTrainers] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedTime, setSelectedTime] = useState(null);
    const [currentMonth, setCurrentMonth] = useState(new Date());
    const [scheduleType, setScheduleType] = useState('ONCE');
    const [selectedDays, setSelectedDays] = useState([]);
    const [appointmentData, setAppointmentData] = useState({
        title: '',
        trainerId: null
    });
    const [loading, setLoading] = useState(true);
    const [editingScheduleId, setEditingScheduleId] = useState(null);

    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const weekDaysForSelection = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

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

    useEffect(() => {
        loadSubscribedTrainers();
        
        // Check if we're editing a schedule
        const scheduleToEdit = localStorage.getItem('scheduleToEdit');
        if (scheduleToEdit) {
            const schedule = JSON.parse(scheduleToEdit);
            loadScheduleForEdit(schedule);
            localStorage.removeItem('scheduleToEdit');
        }
    }, []);

    const loadScheduleForEdit = (schedule) => {
        console.log('Loading schedule for edit:', schedule);
        
        setEditingScheduleId(schedule.id);
        setAppointmentData({
            title: schedule.title,
            trainerId: schedule.trainerId
        });
        setScheduleType(schedule.repeatType);
        
        // Set time
        if (schedule.startTime) {
            const hour = parseInt(schedule.startTime.split(':')[0]);
            setSelectedTime({
                time: `${hour}:00 - ${hour + 1}:00`,
                hour24: `${hour.toString().padStart(2, '0')}:00`
            });
        }
        
        // Set date for ONCE type
        if (schedule.repeatType === 'ONCE' && schedule.date) {
            const date = new Date(schedule.date);
            setSelectedDate(date);
            setCurrentMonth(date);
        }
        
        // Set days for WEEKLY type
        if (schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek) {
            setSelectedDays(schedule.daysOfWeek);
        }
    };

    const loadSubscribedTrainers = async () => {
        try {
            const token = await getToken();
            
            const subsResponse = await fetch(`${BACKEND_URL}/api/trainers/me/subscriptions`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (subsResponse.ok) {
                const subscribedIds = await subsResponse.json();
                
                if (subscribedIds.length > 0) {
                    const trainersResponse = await fetch(`${BACKEND_URL}/api/trainers`, {
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    });
                    
                    if (trainersResponse.ok) {
                        const allTrainers = await trainersResponse.json();
                        const subscribed = allTrainers.filter(t => subscribedIds.includes(t.id));
                        setSubscribedTrainers(subscribed);
                    }
                }
            }
            
            setLoading(false);
        } catch (error) {
            console.error('Error loading trainers:', error);
            setLoading(false);
        }
    };

    const generateCalendarDays = () => {
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth();
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDayOfWeek = firstDay.getDay();
        
        const days = [];
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        for (let i = 0; i < startingDayOfWeek; i++) {
            days.push(null);
        }
        
        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(year, month, day);
            const isPast = date < today;
            days.push({ date, day, isPast });
        }
        
        return days;
    };

    const generateTimeSlots = () => {
        const slots = [];
        for (let hour = 8; hour < 20; hour++) {
            slots.push({
                time: `${hour}:00 - ${hour + 1}:00`,
                hour24: `${hour.toString().padStart(2, '0')}:00`
            });
        }
        return slots;
    };

    const handleDateSelect = (dayObj) => {
        if (dayObj && !dayObj.isPast) {
            setSelectedDate(dayObj.date);
            setSelectedTime(null);
        }
    };

    const handleTimeSelect = (slot) => {
        setSelectedTime(slot);
    };

    const toggleWeekDay = (day) => {
        setSelectedDays(prev => 
            prev.includes(day) 
                ? prev.filter(d => d !== day)
                : [...prev, day]
        );
    };

    const handleSubmitAppointment = async () => {
        if (!appointmentData.title || !appointmentData.trainerId) {
            alert('Please fill in title and select a trainer');
            return;
        }

        // Validation based on schedule type
        if (scheduleType === 'ONCE' && !selectedDate) {
            alert('Please select a date for one-time appointment');
            return;
        }

        if (scheduleType === 'WEEKLY' && selectedDays.length === 0) {
            alert('Please select at least one day for weekly schedule');
            return;
        }

        if (!selectedTime) {
            alert('Please select a time');
            return;
        }

        try {
            const token = await getToken();
            
            const timeOnly = selectedTime.hour24 + ':00';
            
            // Build request based on schedule type
            let scheduleRequest = {
                title: appointmentData.title.trim(),
                startTime: timeOnly,
                repeatType: scheduleType,
                trainerId: appointmentData.trainerId,
                timezone: 'Europe/Zagreb',
                reminderMinutesBefore: 30,
                enabled: true
            };

            // Add type-specific fields
            if (scheduleType === 'ONCE') {
                const year = selectedDate.getFullYear();
                const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
                const day = String(selectedDate.getDate()).padStart(2, '0');
                scheduleRequest.date = `${year}-${month}-${day}`;
            } else if (scheduleType === 'WEEKLY') {
                scheduleRequest.daysOfWeek = selectedDays;
            }

            console.log('Sending schedule request:', scheduleRequest);

            // Determine if creating or updating
            const url = editingScheduleId 
                ? `${BACKEND_URL}/api/schedules/me/${editingScheduleId}`
                : `${BACKEND_URL}/api/schedules/me`;
            
            const method = editingScheduleId ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(scheduleRequest)
            });

            if (response.ok) {
                alert(editingScheduleId ? 'Schedule updated successfully!' : 'Schedule created successfully!');
                
                // Reload the calendar
                if (reloadCalendar) {
                    reloadCalendar();
                }
                
                // Reset form
                setSelectedDate(null);
                setSelectedTime(null);
                setSelectedDays([]);
                setAppointmentData({ title: '', trainerId: null });
                setScheduleType('ONCE');
                setEditingScheduleId(null);
            } else {
                const error = await response.json();
                console.error('Server error:', error);
                alert(`Failed to ${editingScheduleId ? 'update' : 'create'} schedule: ${error.message || JSON.stringify(error)}`);
            }
        } catch (error) {
            console.error('Error with schedule:', error);
            alert(`Failed to ${editingScheduleId ? 'update' : 'create'} schedule: ` + error.message);
        }
    };

    const handleCancelEdit = () => {
        setEditingScheduleId(null);
        setSelectedDate(null);
        setSelectedTime(null);
        setSelectedDays([]);
        setAppointmentData({ title: '', trainerId: null });
        setScheduleType('ONCE');
    };

    const previousMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1));
    };

    const nextMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1));
    };

    if (loading) {
        return (
            <div className={styles.container}>
                <div className={styles.loading}>Loading...</div>
            </div>
        );
    }

    if (subscribedTrainers.length === 0) {
        return (
            <div className={styles.container}>
                <div className={styles.noTrainer}>
                    <h2>👥 No Trainer Subscribed</h2>
                    <p>You need to subscribe to a trainer first before creating a schedule.</p>
                    <button
                        onClick={() => setActiveTab('Trainers')}
                        className={styles.goToTrainersButton}
                    >
                        Go to Trainers
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>
                {editingScheduleId ? 'Edit Practice Schedule' : 'Create Practice Schedule'}
            </h2>

            {/* Schedule Type Selection */}
            <div className={styles.formSection} style={{marginBottom: '25px'}}>
                <h3 className={styles.sectionTitle}>Schedule Type</h3>
                <div className={styles.scheduleTypeButtons}>
                    {['ONCE', 'DAILY', 'WEEKLY'].map(type => (
                        <button
                            key={type}
                            onClick={() => {
                                setScheduleType(type);
                                setSelectedDays([]);
                                setSelectedDate(null);
                            }}
                            className={`${styles.typeButton} ${
                                scheduleType === type ? styles.typeButtonActive : ''
                            }`}
                        >
                            {type === 'ONCE' ? '📅 One Time' : type === 'DAILY' ? '🔄 Daily' : '📆 Weekly'}
                        </button>
                    ))}
                </div>
            </div>

            {/* Weekly Day Selection */}
            {scheduleType === 'WEEKLY' && (
                <div className={styles.formSection} style={{marginBottom: '25px'}}>
                    <h3 className={styles.sectionTitle}>Select Days of Week</h3>
                    <div className={styles.weekDayButtons}>
                        {weekDaysForSelection.map(day => (
                            <button
                                key={day}
                                onClick={() => toggleWeekDay(day)}
                                className={`${styles.weekDayButton} ${
                                    selectedDays.includes(day) ? styles.weekDayButtonActive : ''
                                }`}
                            >
                                {day.substring(0, 3)}
                            </button>
                        ))}
                    </div>
                    {selectedDays.length > 0 && (
                        <p className={styles.selectedDaysText}>
                            Selected: {selectedDays.join(', ')}
                        </p>
                    )}
                </div>
            )}

            {/* Calendar Section - only show for ONCE */}
            {scheduleType === 'ONCE' && (
                <div className={styles.calendarSection}>
                    <div className={styles.calendarHeader}>
                        <button onClick={previousMonth} className={styles.navButton}>←</button>
                        <h3 className={styles.monthTitle}>
                            {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
                        </h3>
                        <button onClick={nextMonth} className={styles.navButton}>→</button>
                    </div>

                    <div className={styles.calendar}>
                        <div className={styles.weekDays}>
                            {dayNames.map(day => (
                                <div key={day} className={styles.weekDay}>{day}</div>
                            ))}
                        </div>
                        <div className={styles.calendarDays}>
                            {generateCalendarDays().map((dayObj, index) => (
                                <button
                                    key={index}
                                    onClick={() => handleDateSelect(dayObj)}
                                    disabled={!dayObj || dayObj.isPast}
                                    className={`${styles.calendarDay} ${
                                        !dayObj ? styles.empty :
                                        dayObj.isPast ? styles.past :
                                        selectedDate?.getDate() === dayObj.day &&
                                        selectedDate?.getMonth() === currentMonth.getMonth()
                                            ? styles.selected
                                            : ''
                                    }`}
                                >
                                    {dayObj?.day}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* Time Slots Section */}
            {(scheduleType === 'DAILY' || scheduleType === 'WEEKLY' || (scheduleType === 'ONCE' && selectedDate)) && (
                <div className={styles.timeSection}>
                    <h3 className={styles.sectionTitle}>
                        Select Time {scheduleType === 'ONCE' && selectedDate ? `- ${selectedDate.toLocaleDateString()}` : ''}
                    </h3>
                    <div className={styles.timeSlots}>
                        {generateTimeSlots().map(slot => (
                            <button
                                key={slot.hour24}
                                onClick={() => handleTimeSelect(slot)}
                                className={`${styles.timeSlot} ${
                                    selectedTime?.hour24 === slot.hour24 ? styles.selectedTime : ''
                                }`}
                            >
                                🕐 {slot.time}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* Appointment Form */}
            {selectedTime && (
                <div className={styles.formSection}>
                    <h3 className={styles.sectionTitle}>Schedule Details</h3>
                    
                    <div className={styles.formGrid}>
                        <div className={styles.formGroup}>
                            <label className={styles.label}>Title *</label>
                            <input
                                type="text"
                                value={appointmentData.title}
                                onChange={(e) => setAppointmentData({...appointmentData, title: e.target.value})}
                                className={styles.input}
                                placeholder="e.g., Morning Meditation"
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label className={styles.label}>Select Trainer *</label>
                            <select
                                value={appointmentData.trainerId || ''}
                                onChange={(e) => setAppointmentData({...appointmentData, trainerId: parseInt(e.target.value)})}
                                className={styles.select}
                            >
                                <option value="">Choose a trainer...</option>
                                {subscribedTrainers.map(trainer => (
                                    <option key={trainer.id} value={trainer.id}>
                                        {trainer.name} {trainer.surname}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className={styles.summary}>
                        <h4 className={styles.summaryTitle}>📋 Schedule Summary</h4>
                        <div className={styles.summaryContent}>
                            <p><strong>Type:</strong> {scheduleType}</p>
                            {scheduleType === 'ONCE' && selectedDate && (
                                <p><strong>Date:</strong> {selectedDate.toLocaleDateString()}</p>
                            )}
                            {scheduleType === 'WEEKLY' && selectedDays.length > 0 && (
                                <p><strong>Days:</strong> {selectedDays.join(', ')}</p>
                            )}
                            <p><strong>Time:</strong> {selectedTime.time}</p>
                            <p><strong>Title:</strong> {appointmentData.title || 'Not set'}</p>
                            <p><strong>Trainer:</strong> {
                                subscribedTrainers.find(t => t.id === appointmentData.trainerId)?.name || 'Not selected'
                            }</p>
                        </div>
                    </div>

                    <button
                        onClick={handleSubmitAppointment}
                        className={styles.submitButton}
                    >
                        {editingScheduleId ? '✅ Update Schedule' : '✅ Create Schedule'}
                    </button>

                    {editingScheduleId && (
                        <button
                            onClick={handleCancelEdit}
                            className={styles.cancelButton}
                        >
                            ❌ Cancel Edit
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}