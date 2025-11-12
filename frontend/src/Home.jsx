import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useCallback } from "react";
import { useNavigate, useLocation} from "react-router-dom";
import styles from "./Home.module.css";

import Header from "./components/home/Header";
import LeftSidebar from "./components/home/LeftSidebar";
import RightSidebar from "./components/home/RightSidebar";
import DashboardTabs from "./components/home/DashboardTabs";
import GeneralInfoGrid from "./components/home/GeneralInfoGrid";

const PasswordResetModal = ({ onPasswordReset, passwordResetData, onPasswordChange }) => {
    const handleSubmit = (e) => {
        e.preventDefault();
        onPasswordReset(e);
    };

    const handleChange = (e) => {
        onPasswordChange(e);
    };

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000,
        }}>
            <div style={{
                backgroundColor: 'white',
                padding: '30px',
                borderRadius: '15px',
                boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)',
                width: '90%',
                maxWidth: '500px',
                textAlign: 'center'
            }}>
                <h2 style={{ color: '#e74c3c', marginBottom: '15px' }}>
                    Password Reset Required
                </h2>
                <p style={{ marginBottom: '25px', color: '#555' }}>
                    This is your first login. You must set a new password before accessing the application.
                </p>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '20px', textAlign: 'left' }}>
                        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
                            New Password:
                        </label>
                        <input
                            type="password"
                            name="newPassword"
                            value={passwordResetData.newPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            style={{
                                width: '100%',
                                padding: '12px',
                                borderRadius: '8px',
                                border: '2px solid #ddd',
                                fontSize: '16px'
                            }}
                            placeholder="Enter new password (min 8 characters)"
                        />
                    </div>

                    <div style={{ marginBottom: '25px', textAlign: 'left' }}>
                        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
                            Confirm Password:
                        </label>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={passwordResetData.confirmPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            style={{
                                width: '100%',
                                padding: '12px',
                                borderRadius: '8px',
                                border: '2px solid #ddd',
                                fontSize: '16px'
                            }}
                            placeholder="Confirm new password"
                        />
                    </div>

                    <button
                        type="submit"
                        style={{
                            backgroundColor: '#27ae60',
                            color: 'white',
                            padding: '12px 30px',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold',
                            width: '100%'
                        }}
                    >
                        Set New Password
                    </button>
                </form>

                <p style={{
                    marginTop: '15px',
                    fontSize: '12px',
                    color: '#777',
                    fontStyle: 'italic'
                }}>
                    You cannot access the application until you set a new password.
                </p>
            </div>
        </div>
    );
};

export default function Home() {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
    const [user, setUser] = useState(null);
    const [responseFromServer, setResponse] = useState("");
    const [loading, setLoading] = useState(true);
    const [requiresPasswordReset, setRequiresPasswordReset] = useState(false);
    const [passwordResetData, setPasswordResetData] = useState({
        newPassword: "",
        confirmPassword: ""
    });
    const navigate = useNavigate();

    const BACKEND_URL = process.env.REACT_APP_BACKEND;

    useEffect(() => {
        const init = async () => {
            const localToken = localStorage.getItem("token");

            try {
                // Auth0 login
                if (isAuthenticated && auth0User) {
                    console.log("Authenticated via Auth0:", auth0User);
                    setUser(auth0User);

                    await sendUserDataToBackend(auth0User);
                    await fetchProtectedResource(); // SDK provides token internally

                    // Auth0 ne treba reset
                    setRequiresPasswordReset(false);
                }
                // Local JWT login
                else if (localToken) {
                    console.log("Authenticated via local JWT");

                    const userRes = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });

                    if (!userRes.ok) throw new Error("Failed to fetch user info");

                    const userData = await userRes.json();
                    console.log("User data from backend:", userData); // Debug log
                    setUser(userData);

                    if (userData.firstLogin !== undefined) {
                        console.log("First login status:", userData.firstLogin);
                        setRequiresPasswordReset(userData.firstLogin);
                    } else {
                        console.warn("firstLogin field not found in user data");
                        try {
                            const resetRes = await fetch(`${BACKEND_URL}/api/user/settings/check-first-login`, {
                                headers: { Authorization: `Bearer ${localToken}` },
                            });
                            if (resetRes.ok) {
                                const requiresReset = await resetRes.json();
                                setRequiresPasswordReset(requiresReset);
                            }
                        } catch (fallbackError) {
                            console.error("Fallback check also failed:", fallbackError);
                        }
                    }

                    await fetchProtectedResource(localToken);
                }
                setLoading(false);
            } catch (err) {
                console.error("Error initializing user:", err);
                setLoading(false);
            }
        };

        init();
    }, [auth0User, isAuthenticated, navigate, BACKEND_URL]);

    const handlePasswordReset = useCallback(async (e) => {
        e.preventDefault();

        if (passwordResetData.newPassword !== passwordResetData.confirmPassword) {
            alert("Passwords don't match!");
            return;
        }

        if (passwordResetData.newPassword.length < 8) {
            alert("Password must be at least 8 characters long!");
            return;
        }

        try {
            const localToken = localStorage.getItem("token");
            const res = await fetch(`${BACKEND_URL}/api/user/settings/first-time-reset`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localToken}`,
                },
                body: JSON.stringify({
                    newPassword: passwordResetData.newPassword
                }),
            });

            if (res.ok) {
                setRequiresPasswordReset(false);
                alert("Password reset successfully! You can now use your new password.");
                setPasswordResetData({ newPassword: "", confirmPassword: "" });

                const userRes = await fetch(`${BACKEND_URL}/api/users/me`, {
                    headers: { Authorization: `Bearer ${localToken}` },
                });
                if (userRes.ok) {
                    const userData = await userRes.json();
                    setUser(userData);
                }
            } else {
                const error = await res.text();
                alert(`Password reset failed: ${error}`);
            }
        } catch (err) {
            console.error("Error resetting password:", err);
            alert("Error resetting password. Please try again.");
        }
    }, [passwordResetData, BACKEND_URL]);

    const handlePasswordChange = useCallback((e) => {
        const { name, value } = e.target;
        setPasswordResetData(prev => ({
            ...prev,
            [name]: value
        }));
    }, []);

    const sendUserDataToBackend = async (auth0User) => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: `${BACKEND_URL}`,
                    scope: "openid profile email",q
                },
            });

            const payload = {
                name: auth0User.given_name || auth0User.name?.split(" ")[0] || "",
                surname: auth0User.family_name || auth0User.name?.split(" ")[1] || "",
                email: auth0User.email,
                lastLogin: new Date().toISOString(),
                isSocialLogin: true,
                auth0Id: auth0User.sub,
            };

            console.log("Sending user payload:", payload);

            const res = await fetch(`${BACKEND_URL}/api/users`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                console.error("Failed to send user data:", res.statusText);
            } else {
                console.log("User data synced successfully with backend");
            }
        } catch (err) {
            console.error("Error sending user data to backend:", err);
        }
    };

    const fetchProtectedResource = async (localToken) => {
        try {
            let token;

            if (localToken) {
                token = localToken; // local JWT
            } else {
                // Auth0 token
                token = await getAccessTokenSilently({
                    authorizationParams: {
                        audience: `${BACKEND_URL}`,
                        scope: "openid profile email",
                    },
                });
            }

            const res = await fetch(`${BACKEND_URL}/protected`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) throw new Error("Failed to fetch protected resource");

            const data = await res.text();
            setResponse(data);
        } catch (err) {
            console.error("Error fetching protected resource:", err);
            setResponse("Error fetching protected resource");
        }
    };

    function HomeLayout() {
        const location = useLocation();
        const [activeTab, setActiveTab] = useState('Fokus');

        const renderTabContent = () => {
            switch (activeTab) {
                case 'Personalized recomendations':
                    return <GeneralInfoGrid />;
                case 'Focus':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Focus Placeholder</h1>
                            <p>Kolege će ovdje implementirati svoj tab.</p>
                        </div>
                    );
                case 'Sleep':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Sleep Placeholder</h1>
                            <p>Kolege će ovdje implementirati svoj tab.</p>
                        </div>
                    );
                case 'Stress':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Stress Placeholder</h1>
                            <p>Kolege će ovdje implementirati svoj tab.</p>
                        </div>
                    );
                case 'Gratitude':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Gratitude Placeholder</h1>
                            <p>Kolege će ovdje implementirati svoj tab.</p>
                        </div>
                    );
                case 'Calendar':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Calendar Placeholder</h1>
                            <p>Kolege će ovdje implementirati Calendar.</p>
                        </div>
                    );
                case 'Journal':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Journal Placeholder</h1>
                            <p>Kolege će ovdje implementirati Journal.</p>
                        </div>
                    );
                case 'Statistics':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Statistics Placeholder</h1>
                            <p>Kolege će ovdje implementirati Statistics.</p>
                        </div>
                    );
                case 'Breathing':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Breathing Placeholder</h1>
                        </div>
                    );
                case 'Account':
                    return (
                        <div className={styles.tabPanel}>
                            <h1>Account Details Placeholder</h1>
                            <p>Ovdje će biti stranica za uređivanje profila.</p>
                        </div>
                    );
                default:
                    return <GeneralInfoGrid />;
            }
        };

        return (
            <div className={styles.layoutContainer}>
                {/* oblaci */}
                <div id="o1"></div>
                <div id="o2"></div>
                <div id="o3"></div>

                <div className={styles.dashboardContentWrapper}>
                    <Header navigate={navigate} user={user} />

                    <div className={styles.mainGrid}>
                        <LeftSidebar
                            user={user}
                            activeTab={activeTab}
                            setActiveTab={setActiveTab}
                        />

                        <div className={styles.mainContent}>
                            <DashboardTabs
                                activeTab={activeTab}
                                setActiveTab={setActiveTab}
                            />

                            {renderTabContent()}
                        </div>

                        <RightSidebar navigate={navigate} />
                    </div>
                </div>
            </div>
        );
    }

    // 🔹 Render logic
    if (loading || isLoading) return <div>Loading...</div>;
    if (!user) return <div>No user found...</div>;

    return (
        <div style={{ position: 'relative' }}>
            {requiresPasswordReset && (
                <PasswordResetModal
                    onPasswordReset={handlePasswordReset}
                    passwordResetData={passwordResetData}
                    onPasswordChange={handlePasswordChange}
                />
            )}

            <div style={requiresPasswordReset ? { filter: 'blur(5px)', pointerEvents: 'none' } : {}}>
                <HomeLayout />
            </div>
        </div>
    );
}