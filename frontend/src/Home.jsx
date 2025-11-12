import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import styles from "./Home.module.css";

import Header from "./components/home/Header";
import LeftSidebar from "./components/home/LeftSidebar";
import RightSidebar from "./components/home/RightSidebar";
import DashboardTabs from "./components/home/DashboardTabs";
import GeneralInfoGrid from "./components/home/GeneralInfoGrid";

export default function Home() {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated, logout } = useAuth0();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [requiresPasswordReset, setRequiresPasswordReset] = useState(false);
    const [passwordResetData, setPasswordResetData] = useState({
        newPassword: "",
        confirmPassword: ""
    });
    const navigate = useNavigate();
    const location = useLocation();
    const hasNavigatedToQuestions = useRef(false);

    const BACKEND_URL = process.env.REACT_APP_BACKEND;

    useEffect(() => {
        const init = async () => {
            const localToken = localStorage.getItem("token");

            try {
                // Auth0 login Google i to
                if (isAuthenticated && auth0User) {
                    console.log("Authenticated via Auth0:", auth0User);
                    setUser(auth0User);

                    const userResponse = await sendUserDataToBackend(auth0User);

                    setRequiresPasswordReset(false);

                    // provjera za jel rjesia kviz
                    if (userResponse && !userResponse.isOnboardingComplete) {
                        if (!hasNavigatedToQuestions.current) {
                            console.log("Onboarding not complete, redirecting to questions");
                            hasNavigatedToQuestions.current = true;
                            navigate("/questions", { replace: true });
                            return;
                        }
                    }

                    console.log("Auth0 user fully onboarded, showing home");
                    setLoading(false);
                }
                // Local JWT login 
                else if (localToken) {
                    console.log("Authenticated via local JWT");

                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });

                    if (!res.ok) throw new Error("Failed to fetch user info");

                    const data = await res.json();
                    console.log("User data from backend:", data);
                    setUser(data);

                    // vidi jel potreban reset lozinke
                    if (data.requiresPasswordReset) {
                        console.log("Password reset required, staying on home page");
                        setRequiresPasswordReset(true);
                        setLoading(false);
                        return; 
                    }

                    // vrijeme za kviz
                    if (!data.isOnboardingComplete) {
                        if (!hasNavigatedToQuestions.current) {
                            console.log("Onboarding not complete, redirecting to questions");
                            hasNavigatedToQuestions.current = true;
                            navigate("/questions", { replace: true });
                            return;
                        }
                    }

                    console.log("Local user fully set up, showing home");
                    setLoading(false);
                } else {
                    setLoading(false);
                }
            } catch (err) {
                console.error("Error initializing user:", err);
                setLoading(false);
            }
        };

        if (!isLoading) {
            init();
        }
    }, [isLoading, isAuthenticated, location.pathname, navigate, BACKEND_URL]);

    const handlePasswordReset = async (e) => {
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
                alert("Password reset successfully!");
                setPasswordResetData({ newPassword: "", confirmPassword: "" });
                setRequiresPasswordReset(false);
                
                // opet uzimamo podatke za provjeru jel rjesia kviz
                const userRes = await fetch(`${BACKEND_URL}/api/users/me`, {
                    headers: { Authorization: `Bearer ${localToken}` },
                });
                
                if (userRes.ok) {
                    const userData = await userRes.json();
                    setUser(userData);
                    
                    // ako nije saljemo ga da rjesi
                    if (!userData.isOnboardingComplete) {
                        console.log("Password reset done, redirecting to questions");
                        navigate("/questions", { replace: true });
                    }
                }
            } else {
                const error = await res.text();
                alert(`Password reset failed: ${error}`);
            }
        } catch (err) {
            console.error("Error resetting password:", err);
            alert("Error resetting password. Please try again.");
        }
    };

    const sendUserDataToBackend = async (auth0User) => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: `${BACKEND_URL}`,
                    scope: "openid profile email",
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

            if (!res.ok) return null;

            const userData = await res.json();
            console.log("User data synced successfully:", userData);
            return userData;
        } catch (err) {
            console.error("Error sending user data to backend:", err);
            return null;
        }
    };

    const handlePasswordChange = (e) => {
        const { name, value } = e.target;
        setPasswordResetData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const renderPasswordResetForm = () => (
        <div style={{
            maxWidth: "500px",
            margin: "50px auto",
            border: "2px solid #f0ad4e",
            padding: "30px",
            borderRadius: "10px",
            backgroundColor: "#fcf8e3",
        }}>
            <h2 style={{ marginBottom: "10px" }}>Welcome! First Time Setup</h2>
            <p style={{ marginBottom: "20px" }}>
                Since this is your first login, please set a new password for your account.
            </p>

            <form onSubmit={handlePasswordReset}>
                <div style={{ marginBottom: "15px" }}>
                    <label style={{ display: "block", marginBottom: "5px", fontWeight: "bold" }}>
                        New Password:
                    </label>
                    <input
                        type="password"
                        name="newPassword"
                        value={passwordResetData.newPassword}
                        onChange={handlePasswordChange}
                        required
                        minLength="8"
                        style={{ 
                            width: "100%", 
                            padding: "10px", 
                            borderRadius: "4px", 
                            border: "1px solid #ccc",
                            fontSize: "14px"
                        }}
                        placeholder="Enter new password (min 8 characters)"
                    />
                </div>

                <div style={{ marginBottom: "20px" }}>
                    <label style={{ display: "block", marginBottom: "5px", fontWeight: "bold" }}>
                        Confirm Password:
                    </label>
                    <input
                        type="password"
                        name="confirmPassword"
                        value={passwordResetData.confirmPassword}
                        onChange={handlePasswordChange}
                        required
                        minLength="8"
                        style={{ 
                            width: "100%", 
                            padding: "10px", 
                            borderRadius: "4px", 
                            border: "1px solid #ccc",
                            fontSize: "14px"
                        }}
                        placeholder="Confirm new password"
                    />
                </div>

                <button
                    type="submit"
                    style={{
                        width: "100%",
                        backgroundColor: "#5cb85c",
                        color: "white",
                        padding: "12px 20px",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontSize: "16px",
                        fontWeight: "bold"
                    }}
                >
                    Set Password & Continue
                </button>
            </form>
        </div>
    );

    const handleLogout = () => {
        try {
            // Clear any locally stored JWT
            localStorage.removeItem("token");
            
            if (isAuthenticated) {
            // ✅ Auth0 logout
            import("@auth0/auth0-react").then(({ useAuth0 }) => {
                // can't use hooks dynamically, so simpler direct redirect:
                window.location.href = `${window.location.origin}/login`;
            });
            } else {
            // ✅ Local logout
            navigate("/login");
            }
        } catch (err) {
            console.error("Logout error:", err);
            navigate("/login");
        }
    };

    function HomeLayout() {
        const [activeTab, setActiveTab] = useState('Personalized recomendations');

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
                            handleLogout={handleLogout} 
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

    if (loading || isLoading) return <div>Loading...</div>;
    if (!user) return <div>No user found...</div>;

    if (requiresPasswordReset) {
        return renderPasswordResetForm();
    }

    return <HomeLayout />;
}