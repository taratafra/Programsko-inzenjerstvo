import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from './TrainerDashboard.module.css';

export default function TrainerDashboard({ setActiveTab }) {
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const [clients, setClients] = useState([]);
    const [schedules, setSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeSection, setActiveSection] = useState('overview'); // overview, clients, schedules

    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

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
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            const token = await getToken();
            
            // Load client IDs (users subscribed to me as a trainer)
            const clientIdsResponse = await fetch(`${BACKEND_URL}/api/trainers/me/users`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (clientIdsResponse.ok) {
                const clientIds = await clientIdsResponse.json();
                
                // Fetch full user details for each client
                if (clientIds.length > 0) {
                    const clientDetailsPromises = clientIds.map(async (userId) => {
                        const userResponse = await fetch(`${BACKEND_URL}/api/users/${userId}`, {
                            headers: {
                                'Authorization': `Bearer ${token}`
                            }
                        });
                        if (userResponse.ok) {
                            return await userResponse.json();
                        }
                        return null;
                    });
                    
                    const clientDetails = await Promise.all(clientDetailsPromises);
                    setClients(clientDetails.filter(client => client !== null));
                } else {
                    setClients([]);
                }
            }

            // Load schedules for trainer
            const schedulesResponse = await fetch(`${BACKEND_URL}/api/schedules/trainer/me`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (schedulesResponse.ok) {
                const schedulesData = await schedulesResponse.json();
                setSchedules(schedulesData);
            }
            
            setLoading(false);
        } catch (error) {
            console.error('Error loading dashboard data:', error);
            setLoading(false);
        }
    };

    const getUpcomingSchedules = () => {
        const now = new Date();
        return schedules
            .filter(s => s.enabled)
            .filter(s => {
                if (s.repeatType === 'ONCE') {
                    return new Date(s.date) >= now;
                }
                return true;
            })
            .slice(0, 5);
    };

    if (loading) {
        return (
            <div className={styles.container}>
                <div className={styles.loading}>Loading dashboard...</div>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>💼 Trainer Dashboard</h2>

            {/* Section Navigation */}
            <div className={styles.sectionNav}>
                <button
                    onClick={() => setActiveSection('overview')}
                    className={`${styles.sectionButton} ${
                        activeSection === 'overview' ? styles.sectionButtonActive : ''
                    }`}
                >
                    📊 Overview
                </button>
                <button
                    onClick={() => setActiveSection('clients')}
                    className={`${styles.sectionButton} ${
                        activeSection === 'clients' ? styles.sectionButtonActive : ''
                    }`}
                >
                    👥 Clients ({clients.length})
                </button>
                <button
                    onClick={() => setActiveSection('schedules')}
                    className={`${styles.sectionButton} ${
                        activeSection === 'schedules' ? styles.sectionButtonActive : ''
                    }`}
                >
                    📅 Schedules ({schedules.length})
                </button>
            </div>

            {/* Overview Section */}
            {activeSection === 'overview' && (
                <div className={styles.overviewSection}>
                    <div className={styles.statsGrid}>
                        <div className={styles.statCard}>
                            <div className={styles.statIcon}>👥</div>
                            <div className={styles.statNumber}>{clients.length}</div>
                            <div className={styles.statLabel}>Total Clients</div>
                        </div>
                        
                        <div className={styles.statCard}>
                            <div className={styles.statIcon}>📅</div>
                            <div className={styles.statNumber}>{schedules.length}</div>
                            <div className={styles.statLabel}>Active Schedules</div>
                        </div>
                        
                        <div className={styles.statCard}>
                            <div className={styles.statIcon}>✅</div>
                            <div className={styles.statNumber}>
                                {schedules.filter(s => s.enabled).length}
                            </div>
                            <div className={styles.statLabel}>Enabled Schedules</div>
                        </div>

                        <div className={styles.statCard}>
                            <div className={styles.statIcon}>🔄</div>
                            <div className={styles.statNumber}>
                                {schedules.filter(s => s.repeatType === 'WEEKLY').length}
                            </div>
                            <div className={styles.statLabel}>Recurring Sessions</div>
                        </div>
                    </div>

                    {/* Upcoming Sessions */}
                    <div className={styles.upcomingSection}>
                        <h3 className={styles.sectionTitle}>📆 Upcoming Sessions</h3>
                        {getUpcomingSchedules().length > 0 ? (
                            <div className={styles.scheduleList}>
                                {getUpcomingSchedules().map(schedule => (
                                    <div key={schedule.id} className={styles.scheduleCard}>
                                        <div className={styles.scheduleHeader}>
                                            <h4>{schedule.title}</h4>
                                            <span className={styles.scheduleBadge}>
                                                {schedule.repeatType}
                                            </span>
                                        </div>
                                        <div className={styles.scheduleDetails}>
                                            <p>🕐 {schedule.startTime}</p>
                                            {schedule.repeatType === 'ONCE' && (
                                                <p>📅 {new Date(schedule.date).toLocaleDateString()}</p>
                                            )}
                                            {schedule.repeatType === 'WEEKLY' && (
                                                <p>📆 {schedule.daysOfWeek?.join(', ')}</p>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className={styles.emptyMessage}>No upcoming sessions scheduled</p>
                        )}
                    </div>
                </div>
            )}

            {/* Clients Section */}
            {activeSection === 'clients' && (
                <div className={styles.clientsSection}>
                    <h3 className={styles.sectionTitle}>👥 Your Clients</h3>
                    {clients.length > 0 ? (
                        <div className={styles.clientsGrid}>
                            {clients.map(client => (
                                <div key={client.id} className={styles.clientCard}>
                                    <div className={styles.clientAvatar}>
                                        {client.name?.[0]}{client.surname?.[0]}
                                    </div>
                                    <div className={styles.clientInfo}>
                                        <h4>{client.name} {client.surname}</h4>
                                        <p>{client.email}</p>
                                        {client.subscriptionDate && (
                                            <p className={styles.subscriptionDate}>
                                                Subscribed: {new Date(client.subscriptionDate).toLocaleDateString()}
                                            </p>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className={styles.emptyState}>
                            <p>No clients subscribed yet</p>
                        </div>
                    )}
                </div>
            )}

            {/* Schedules Section */}
            {activeSection === 'schedules' && (
                <div className={styles.schedulesSection}>
                    <h3 className={styles.sectionTitle}>📅 All Schedules</h3>
                    {schedules.length > 0 ? (
                        <div className={styles.schedulesList}>
                            {schedules.map(schedule => (
                                <div key={schedule.id} className={styles.scheduleItem}>
                                    <div className={styles.scheduleInfo}>
                                        <h4>{schedule.title}</h4>
                                        <div className={styles.scheduleMetadata}>
                                            <span className={`${styles.badge} ${styles[schedule.repeatType.toLowerCase()]}`}>
                                                {schedule.repeatType}
                                            </span>
                                            <span className={`${styles.badge} ${schedule.enabled ? styles.enabled : styles.disabled}`}>
                                                {schedule.enabled ? 'Enabled' : 'Disabled'}
                                            </span>
                                        </div>
                                        <div className={styles.scheduleTime}>
                                            <p>🕐 {schedule.startTime}</p>
                                            {schedule.repeatType === 'ONCE' && schedule.date && (
                                                <p>📅 {new Date(schedule.date).toLocaleDateString()}</p>
                                            )}
                                            {schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek && (
                                                <p>📆 {schedule.daysOfWeek.join(', ')}</p>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className={styles.emptyState}>
                            <p>No schedules created yet</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
