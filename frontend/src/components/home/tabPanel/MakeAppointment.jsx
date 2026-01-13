import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from './MakeAppointment.module.css';

export default function MakeAppointment({ setActiveTab }) {
    const { getAccessTokenSilently } = useAuth0();
    const [subscribedTrainers, setSubscribedTrainers] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedTime, setSelectedTime] = useState(null);
    const [currentMonth, setCurrentMonth] = useState(new Date());
    const [appointmentData, setAppointmentData] = useState({
        firstName: '',
        lastName: '',
        trainerId: null
    });
    const [loading, setLoading] = useState(true);

    const BACKEND_URL = process.env.REACT_APP_BACKEND;

    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    useEffect(() => {
        loadSubscribedTrainers();
    }, []);

    const loadSubscribedTrainers = async () => {
        try {
            const token = await getAccessTokenSilently();
            
            // Get primary trainer
            const response = await fetch(`${BACKEND_URL}/api/trainers/me/primary`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.trainerId) {
                    // Fetch trainer details
                    const trainerResponse = await fetch(`${BACKEND_URL}/api/users/${data.trainerId}`, {
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    });
                    
                    if (trainerResponse.ok) {
                        const trainer = await trainerResponse.json();
                        setSubscribedTrainers([trainer]);
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

    const handleSubmitAppointment = async () => {
        if (!appointmentData.firstName || !appointmentData.lastName || !appointmentData.trainerId) {
            alert('Please fill in all fields');
            return;
        }

        try {
            const token = await getAccessTokenSilently();
            
            const startDateTime = new Date(selectedDate);
            const [hours] = selectedTime.hour24.split(':');
            startDateTime.setHours(parseInt(hours), 0, 0, 0);

            const scheduleRequest = {
                title: `Appointment: ${appointmentData.firstName} ${appointmentData.lastName}`,
                startTime: startDateTime.toISOString(),
                repeatType: 'ONCE',
                daysOfWeek: [],
                timezone: 'Europe/Zagreb',
                reminderMinutesBefore: 30,
                enabled: true
            };

            const response = await fetch(`${BACKEND_URL}/api/schedules/me`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(scheduleRequest)
            });

            if (response.ok) {
                alert('Appointment scheduled successfully!');
                
                setSelectedDate(null);
                setSelectedTime(null);
                setAppointmentData({ firstName: '', lastName: '', trainerId: null });
            } else {
                const error = await response.json();
                alert(`Failed to schedule appointment: ${error.message || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Error creating appointment:', error);
            alert('Failed to schedule appointment');
        }
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
                    <p>You need to subscribe to a trainer first before making an appointment.</p>
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
            <h2 className={styles.title}>Make an Appointment</h2>

            {/* Calendar Section */}
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

            {/* Time Slots Section */}
            {selectedDate && (
                <div className={styles.timeSection}>
                    <h3 className={styles.sectionTitle}>
                        Select Time - {selectedDate.toLocaleDateString()}
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
                    <h3 className={styles.sectionTitle}>Appointment Details</h3>
                    
                    <div className={styles.formGrid}>
                        <div className={styles.formGroup}>
                            <label className={styles.label}>First Name</label>
                            <input
                                type="text"
                                value={appointmentData.firstName}
                                onChange={(e) => setAppointmentData({...appointmentData, firstName: e.target.value})}
                                className={styles.input}
                                placeholder="Enter your first name"
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label className={styles.label}>Last Name</label>
                            <input
                                type="text"
                                value={appointmentData.lastName}
                                onChange={(e) => setAppointmentData({...appointmentData, lastName: e.target.value})}
                                className={styles.input}
                                placeholder="Enter your last name"
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label className={styles.label}>Select Trainer</label>
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
                        <h4 className={styles.summaryTitle}>📋 Appointment Summary</h4>
                        <div className={styles.summaryContent}>
                            <p><strong>Date:</strong> {selectedDate.toLocaleDateString()}</p>
                            <p><strong>Time:</strong> {selectedTime.time}</p>
                            <p><strong>Name:</strong> {appointmentData.firstName} {appointmentData.lastName}</p>
                            <p><strong>Trainer:</strong> {
                                subscribedTrainers.find(t => t.id === appointmentData.trainerId)?.name || 'Not selected'
                            }</p>
                        </div>
                    </div>

                    <button
                        onClick={handleSubmitAppointment}
                        className={styles.submitButton}
                    >
                        Confirm Appointment
                    </button>
                </div>
            )}
        </div>
    );
}