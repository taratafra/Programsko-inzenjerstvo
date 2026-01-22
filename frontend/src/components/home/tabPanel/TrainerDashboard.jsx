import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import MetricChart from './Statistics/MetricChart';
import EmotionsChart from './Statistics/EmotionsChart';
import TextualDataDisplay from './Statistics/TextualDataDisplay';
import styles from './TrainerDashboard.module.css';

export default function TrainerDashboard({ setActiveTab }) {
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const [clients, setClients] = useState([]);
    const [schedules, setSchedules] = useState([]);
    const [content, setContent] = useState([]);
    const [averageRating, setAverageRating] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeSection, setActiveSection] = useState('overview');
    
    const [selectedClient, setSelectedClient] = useState(null);
    const [clientCheckIns, setClientCheckIns] = useState([]);
    const [loadingStats, setLoadingStats] = useState(false);

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
                if (!localToken) throw new Error("No authentication token found");
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

    // Debug effect to monitor schedules state
    useEffect(() => {
        console.log('📅 Current schedules in state:', schedules);
        console.log('📅 Number of schedules:', schedules.length);
        if (schedules.length > 0) {
            console.log('📅 First schedule details:', schedules[0]);
            console.log('📅 Upcoming schedules:', getUpcomingSchedules());
        }
    }, [schedules]);

    const loadDashboardData = async () => {
        try {
            const token = await getToken();
            
            // Fetch clients first
            const clientsRes = await fetch(`${BACKEND_URL}/api/trainers/me/clients`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            let clientsData = [];
            if (clientsRes.ok) {
                clientsData = await clientsRes.json();
                console.log('✅ Clients loaded:', clientsData);
                setClients(clientsData);
            } else {
                console.error('❌ Clients fetch failed:', clientsRes.status, await clientsRes.text());
            }

            // Fetch schedules where trainer is the trainer (client appointments)
            const schedulesRes = await fetch(`${BACKEND_URL}/api/schedules/trainer/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (schedulesRes.ok) {
                const schedulesData = await schedulesRes.json();
                console.log('✅ Schedules loaded:', schedulesData);
                console.log('📊 Schedules count:', schedulesData.length);
                console.log('📊 Schedules full data:', JSON.stringify(schedulesData, null, 2));
                setSchedules(Array.isArray(schedulesData) ? schedulesData : []);
            } else {
                console.error('❌ Schedules fetch failed:', schedulesRes.status, await schedulesRes.text());
                setSchedules([]);
            }

            // Fetch content
            const contentRes = await fetch(`${BACKEND_URL}/api/videos/trainer/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (contentRes.ok) {
                const contentData = await contentRes.json();
                console.log('✅ Content loaded:', contentData);
                setContent(contentData);
            } else {
                console.error('❌ Content fetch failed:', contentRes.status);
            }

            // Fetch average rating
            const ratingRes = await fetch(`${BACKEND_URL}/api/videos/trainer/me/average-rating`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (ratingRes.ok) {
                const ratingData = await ratingRes.json();
                console.log('✅ Rating loaded:', ratingData);
                setAverageRating(ratingData);
            } else {
                console.error('❌ Rating fetch failed:', ratingRes.status);
            }
            
            setLoading(false);
        } catch (error) {
            console.error('❌ Error loading dashboard data:', error);
            setLoading(false);
        }
    };

    const loadClientStatistics = async (clientId) => {
        setLoadingStats(true);
        try {
            const token = await getToken();
            const response = await fetch(`${BACKEND_URL}/api/mood-checkins/${clientId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                const data = await response.json();
                setClientCheckIns(Array.isArray(data) ? data : []);
            } else {
                setClientCheckIns([]);
            }
        } catch (error) {
            console.error('Error loading client statistics:', error);
            setClientCheckIns([]);
        } finally {
            setLoadingStats(false);
        }
    };

    const handleClientSelect = (client) => {
        setSelectedClient(client);
        setActiveSection('statistics');
        loadClientStatistics(client.id);
    };

    const handleBackToClients = () => {
        setSelectedClient(null);
        setClientCheckIns([]);
        setActiveSection('clients');
    };

    const handleDeleteContent = async (contentId) => {
        if (!window.confirm('Are you sure you want to delete this content?')) return;

        try {
            const token = await getToken();
            const response = await fetch(`${BACKEND_URL}/api/videos/${contentId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok || response.status === 204) {
                setContent(content.filter(c => c.id !== contentId));
                alert('Content deleted successfully');
            } else {
                alert('Failed to delete content');
            }
        } catch (error) {
            console.error('Error deleting content:', error);
            alert('Error deleting content');
        }
    };

    const getUpcomingSchedules = () => {
        console.log('🔍 Getting upcoming schedules from:', schedules);
        
        if (!schedules || schedules.length === 0) {
            console.log('⚠️ No schedules to filter');
            return [];
        }

        const now = new Date();
        const today = now.toISOString().split('T')[0]; // YYYY-MM-DD format
        
        const filtered = schedules.filter(s => {
            console.log(`📋 Checking schedule: ${s.title}, enabled: ${s.enabled}, repeatType: ${s.repeatType}`);
            
            // Only show enabled schedules
            if (!s.enabled) {
                console.log(`  ❌ Schedule disabled`);
                return false;
            }
            
            if (s.repeatType === 'ONCE') {
                const isUpcoming = s.date && s.date >= today;
                console.log(`  📅 ONCE schedule: date=${s.date}, isUpcoming=${isUpcoming}`);
                return isUpcoming;
            }
            
            // DAILY and WEEKLY schedules are always considered upcoming
            console.log(`  ✅ ${s.repeatType} schedule - always upcoming`);
            return true;
        });

        console.log('✅ Filtered upcoming schedules:', filtered);
        return filtered.slice(0, 5);
    };

    const hasDataForMetric = (dataKey) => clientCheckIns.some(c => c[dataKey] != null);
    const hasEmotionsData = () => clientCheckIns.some(c => c.emotions?.length > 0);
    const hasTextualData = () => clientCheckIns.some(c => c.caffeineIntake || c.alcoholIntake || c.physicalActivity || c.notes);

    if (loading) {
        return (
            <div className={styles.container}>
                <div className={styles.loading}>Loading dashboard...</div>
            </div>
        );
    }

    const upcomingSchedules = getUpcomingSchedules();

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>💼 Trainer Dashboard</h2>

            <div className={styles.sectionNav}>
                <button
                    onClick={() => setActiveSection('overview')}
                    className={`${styles.sectionButton} ${activeSection === 'overview' ? styles.sectionButtonActive : ''}`}
                >
                    📊 Overview
                </button>
                <button
                    onClick={() => {
                        setActiveSection('clients');
                        setSelectedClient(null);
                    }}
                    className={`${styles.sectionButton} ${activeSection === 'clients' ? styles.sectionButtonActive : ''}`}
                >
                    👥 Clients ({clients.length})
                </button>
                <button
                    onClick={() => setActiveSection('content')}
                    className={`${styles.sectionButton} ${activeSection === 'content' ? styles.sectionButtonActive : ''}`}
                >
                    🎬 Content ({content.length})
                </button>
            </div>

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
                            <div className={styles.statNumber}>{upcomingSchedules.length}</div>
                            <div className={styles.statLabel}>Upcoming Sessions</div>
                        </div>
                        
                        <div className={styles.statCard}>
                            <div className={styles.statIcon}>⭐</div>
                            <div className={styles.statNumber}>
                                {averageRating ? averageRating.averageRating.toFixed(1) : 'N/A'}
                            </div>
                            <div className={styles.statLabel}>Average Rating</div>
                        </div>
                    </div>

                    <div className={styles.upcomingSection}>
                        <h3 className={styles.sectionTitle}>📆 Upcoming Sessions</h3>
                        {upcomingSchedules.length > 0 ? (
                            <div className={styles.scheduleList}>
                                {upcomingSchedules.map(schedule => (
                                    <div key={schedule.id} className={styles.scheduleCard}>
                                        <div className={styles.scheduleHeader}>
                                            <h4>{schedule.title}</h4>
                                            <span className={styles.scheduleBadge}>{schedule.repeatType}</span>
                                        </div>
                                        <div className={styles.scheduleDetails}>
                                            <p>🕐 {schedule.startTime}</p>
                                            {schedule.repeatType === 'ONCE' && schedule.date && (
                                                <p>📅 {new Date(schedule.date).toLocaleDateString()}</p>
                                            )}
                                            {schedule.repeatType === 'WEEKLY' && schedule.daysOfWeek && schedule.daysOfWeek.length > 0 && (
                                                <p>📆 {schedule.daysOfWeek.join(', ')}</p>
                                            )}
                                            {schedule.repeatType === 'DAILY' && (
                                                <p>📆 Every day</p>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className={styles.emptyMessage}>
                                <p>No upcoming sessions scheduled</p>
                                {schedules.length > 0 && (
                                    <p style={{ fontSize: '0.9em', marginTop: '0.5rem' }}>
                                        (You have {schedules.length} total schedule(s), but none are upcoming or enabled)
                                    </p>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {activeSection === 'clients' && (
                <div className={styles.clientsSection}>
                    <h3 className={styles.sectionTitle}>👥 Your Clients</h3>
                    {clients.length > 0 ? (
                        <div className={styles.clientsGrid}>
                            {clients.map(client => (
                                <div 
                                    key={client.id} 
                                    className={styles.clientCard}
                                    onClick={() => handleClientSelect(client)}
                                    style={{ cursor: 'pointer' }}
                                >
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
                                        <p className={styles.viewStats}>📊 Click to view statistics</p>
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

            {activeSection === 'statistics' && selectedClient && (
                <div className={styles.statisticsSection}>
                    <div className={styles.statisticsHeader}>
                        <button onClick={handleBackToClients} className={styles.backButton}>
                            ← Back to Clients
                        </button>
                        <h3 className={styles.sectionTitle}>
                            📊 Statistics for {selectedClient.name} {selectedClient.surname}
                        </h3>
                    </div>

                    {loadingStats ? (
                        <div className={styles.loading}>Loading statistics...</div>
                    ) : clientCheckIns.length === 0 ? (
                        <p className={styles.noData}>No mood check-in data available for this client yet.</p>
                    ) : (
                        <div className={styles.chartsContainer}>
                            {hasDataForMetric('moodScore') && <MetricChart title="Mood" data={clientCheckIns} dataKey="moodScore" />}
                            {hasDataForMetric('sleepQuality') && <MetricChart title="Sleep Quality" data={clientCheckIns} dataKey="sleepQuality" />}
                            {hasDataForMetric('stressLevel') && <MetricChart title="Stress Level" data={clientCheckIns} dataKey="stressLevel" />}
                            {hasDataForMetric('focusLevel') && <MetricChart title="Focus Level" data={clientCheckIns} dataKey="focusLevel" />}
                            {hasEmotionsData() && <EmotionsChart data={clientCheckIns} />}
                            {hasTextualData() && <TextualDataDisplay data={clientCheckIns} />}
                        </div>
                    )}
                </div>
            )}

            {activeSection === 'content' && (
                <div className={styles.contentSection}>
                    <h3 className={styles.sectionTitle}>🎬 Your Content</h3>
                    {content.length > 0 ? (
                        <div className={styles.contentGrid}>
                            {content.map(item => (
                                <div key={item.id} className={styles.contentCard}>
                                    <div className={styles.contentHeader}>
                                        <h4>{item.title}</h4>
                                        <span className={`${styles.contentBadge} ${styles[item.type.toLowerCase()]}`}>
                                            {item.type}
                                        </span>
                                    </div>
                                    <p className={styles.contentDescription}>{item.description}</p>
                                    <div className={styles.contentMetadata}>
                                        {item.duration && <span>⏱️ {item.duration} min</span>}
                                        {item.level && <span>📊 {item.level}</span>}
                                        {item.goal && <span>🎯 {item.goal}</span>}
                                    </div>
                                    <div className={styles.contentActions}>
                                        <button 
                                            className={styles.deleteButton}
                                            onClick={() => handleDeleteContent(item.id)}
                                        >
                                            🗑️ Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className={styles.emptyState}>
                            <p>No content uploaded yet</p>
                            <p>Upload videos, audios, or blogs to share with your clients</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}