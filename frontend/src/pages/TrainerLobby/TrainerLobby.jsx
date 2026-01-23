import { useState, useEffect, useCallback, useRef } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { useNavigate } from "react-router-dom";
import styles from "./TrainerLobby.module.css";
import CloudBackground from "../../components/backgrounds/CloudyBackground";

export default function TrainerLobby() {
    const [trainerStatus, setTrainerStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const { getAccessTokenSilently, isAuthenticated, logout } = useAuth0();
    const navigate = useNavigate();
    const intervalRef = useRef(null);

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

    const handleLogout = useCallback(() => {
        if (isAuthenticated) {
            logout({ returnTo: window.location.origin });
        } else {
            localStorage.removeItem("token");
            navigate("/login");
        }
    }, [isAuthenticated, logout, navigate]);

    useEffect(() => {
        const checkTrainerStatus = async () => {
            try {
                let token;
                const localToken = localStorage.getItem("token");

                if (isAuthenticated) {
                    token = await getAccessTokenSilently({
                        authorizationParams: {
                            audience: AUDIENCE,
                            scope: "openid profile email",
                        },
                    });
                } else if (localToken) {
                    token = localToken;
                } else {
                    navigate("/login");
                    return;
                }

                const res = await fetch(`${BACKEND_URL}/api/trainers/me`, {
                    headers: { Authorization: `Bearer ${token}` },
                });

                if (res.ok) {
                    const data = await res.json();
                    setTrainerStatus(data);

                    if (data.approved) {
                        if (intervalRef.current) {
                            clearInterval(intervalRef.current);
                            intervalRef.current = null;
                        }
                        navigate("/home", { replace: true });
                        return;
                    }
                } else if (res.status === 404) {
                    console.error("Trainer record not found");
                    setTrainerStatus({ approved: false, email: "Unknown" });
                } else if (res.status === 401 || res.status === 403) {
                    handleLogout();
                } else {
                    console.error("Failed to fetch trainer status:", res.status);
                    setTrainerStatus({ approved: false, email: "Unknown" });
                }
            } catch (err) {
                console.error("Error checking trainer status:", err);
            } finally {
                setLoading(false);
            }
        };

        checkTrainerStatus();
        intervalRef.current = setInterval(checkTrainerStatus, 10000);
        
        return () => {
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
        };
    }, [isAuthenticated, getAccessTokenSilently, navigate, BACKEND_URL, AUDIENCE, handleLogout]);

    if (loading) {
        return (
            <CloudBackground>
                <div className={styles.lobbyContainer}>
                    <div className={styles.loadingSpinner}></div>
                    <p>Loading...</p>
                </div>
            </CloudBackground>
        );
    }

    return (
        <CloudBackground>
            <div className={styles.lobbyContainer}>
                <div className={styles.lobbyCard}>
                    <h1 className={styles.title}>Trainer Application Pending</h1>

                    <p className={styles.message}>
                        Thank you for applying to become a trainer! Your application is currently
                        being reviewed by our admin team.
                    </p>

                    <div className={styles.actions}>
                        <button className={styles.logoutBtn} onClick={handleLogout}>
                            Logout
                        </button>
                    </div>
                </div>
            </div>
        </CloudBackground>
    );
}