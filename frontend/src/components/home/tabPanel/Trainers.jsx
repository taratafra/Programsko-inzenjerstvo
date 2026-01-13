import { useState, useEffect } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import styles from './Trainers.module.css';

export default function Trainers() {
    const { getAccessTokenSilently } = useAuth0();
    const [trainers, setTrainers] = useState([]);
    const [subscribedTrainers, setSubscribedTrainers] = useState([]);
    const [primaryTrainerId, setPrimaryTrainerId] = useState(null);
    const [loading, setLoading] = useState(true);

    const BACKEND_URL = process.env.REACT_APP_BACKEND;

    useEffect(() => {
        loadTrainers();
        loadSubscribedTrainers();
    }, []);

    const loadTrainers = async () => {
        try {
            const token = await getAccessTokenSilently();
            
            const response = await fetch(`${BACKEND_URL}/api/users/trainers`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            if (response.ok) {
                const allTrainers = await response.json();
                setTrainers(allTrainers);
            }
            
            setLoading(false);
        } catch (error) {
            console.error('Error loading trainers:', error);
            setLoading(false);
        }
    };

    const loadSubscribedTrainers = async () => {
        try {
            const token = await getAccessTokenSilently();
            
            const response = await fetch(`${BACKEND_URL}/api/trainers/me/primary`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.trainerId) {
                    setPrimaryTrainerId(data.trainerId);
                }
            }
        } catch (error) {
            console.error('Error loading subscribed trainers:', error);
        }
    };

    const handleSubscribe = async (trainerId) => {
        try {
            const token = await getAccessTokenSilently();
            
            const response = await fetch(`${BACKEND_URL}/api/trainers/me/primary`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ trainerId })
            });

            if (response.ok) {
                const trainer = trainers.find(t => t.id === trainerId);
                setSubscribedTrainers(prev => [...prev, { ...trainer, isSubscribed: true }]);
                
                if (!primaryTrainerId) {
                    setPrimaryTrainerId(trainerId);
                }
                
                await loadSubscribedTrainers();
            }
        } catch (error) {
            console.error('Error subscribing:', error);
            alert('Failed to subscribe to trainer');
        }
    };

    const handleUnsubscribe = async (trainerId) => {
        try {
            const token = await getAccessTokenSilently();
            
            const response = await fetch(`${BACKEND_URL}/api/trainers/me/${trainerId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok || response.status === 204) {
                setSubscribedTrainers(prev => prev.filter(t => t.id !== trainerId));
                
                if (primaryTrainerId === trainerId) {
                    setPrimaryTrainerId(null);
                }
            }
        } catch (error) {
            console.error('Error unsubscribing:', error);
            alert('Failed to unsubscribe from trainer');
        }
    };

    const setPrimary = async (trainerId) => {
        try {
            const token = await getAccessTokenSilently();
            
            const response = await fetch(`${BACKEND_URL}/api/trainers/me/primary`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ trainerId })
            });

            if (response.ok) {
                setPrimaryTrainerId(trainerId);
            }
        } catch (error) {
            console.error('Error setting primary trainer:', error);
            alert('Failed to set primary trainer');
        }
    };

    if (loading) {
        return (
            <div className={styles.container}>
                <div className={styles.loading}>Loading trainers...</div>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>Available Trainers</h2>
            <div className={styles.trainersGrid}>
                {trainers.map(trainer => {
                    const isSubscribed = subscribedTrainers.some(t => t.id === trainer.id);
                    const isPrimary = primaryTrainerId === trainer.id;
                    
                    return (
                        <div key={trainer.id} className={styles.trainerCard}>
                            <div className={styles.trainerHeader}>
                                <img
                                    src={trainer.picture || `https://i.pravatar.cc/150?u=${trainer.id}`}
                                    alt={trainer.name}
                                    className={styles.trainerAvatar}
                                />
                                {isPrimary && (
                                    <span className={styles.primaryBadge}>Primary</span>
                                )}
                            </div>
                            
                            <h3 className={styles.trainerName}>
                                {trainer.name} {trainer.surname}
                            </h3>
                            <p className={styles.trainerSpecialty}>
                                {trainer.specialty || 'Mindfulness Trainer'}
                            </p>
                            <p className={styles.trainerEmail}>{trainer.email}</p>
                            
                            <div className={styles.trainerActions}>
                                {isSubscribed ? (
                                    <>
                                        <div className={styles.subscribedLabel}>
                                            ✓ Subscribed
                                        </div>
                                        <div className={styles.buttonGroup}>
                                            {!isPrimary && (
                                                <button
                                                    onClick={() => setPrimary(trainer.id)}
                                                    className={styles.primaryButton}
                                                >
                                                    Set Primary
                                                </button>
                                            )}
                                            <button
                                                onClick={() => handleUnsubscribe(trainer.id)}
                                                className={styles.unsubscribeButton}
                                            >
                                                Unsubscribe
                                            </button>
                                        </div>
                                    </>
                                ) : (
                                    <button
                                        onClick={() => handleSubscribe(trainer.id)}
                                        className={styles.subscribeButton}
                                    >
                                        Subscribe
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}