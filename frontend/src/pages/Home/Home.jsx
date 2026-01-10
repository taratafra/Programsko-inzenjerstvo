import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import styles from "./Home.module.css";

import Header from "../../components/home/Header";
import LeftSidebar from "../../components/home/LeftSidebar";
import RightSidebar from "../../components/home/RightSidebar";
import DashboardTabs from "../../components/home/DashboardTabs";
import GeneralInfoGrid from "../../components/home/GeneralInfoGrid";

import Settings from "../../components/home/tabPanel/Settings";

export default function Home() {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated, logout } = useAuth0();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const location = useLocation();
    const hasNavigatedToQuestions = useRef(false);

    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

    useEffect(() => {
        const init = async () => {
            const localToken = localStorage.getItem("token");

            try {
                // Auth0 login Google i to
                if (isAuthenticated && auth0User) {
                    //console.log("Authenticated via Auth0:", auth0User);
                    setUser(auth0User);

                    const userResponse = await sendUserDataToBackend(auth0User);

                    if (userResponse) {
                        setUser(userResponse);
                    } else {
                        setUser(auth0User); // Fallback
                    }

                    // provjera za jel rjesia kviz
                    if (userResponse && !userResponse.isOnboardingComplete) {
                        if (!hasNavigatedToQuestions.current) {
                            //console.log("Onboarding not complete, redirecting to questions");
                            hasNavigatedToQuestions.current = true;
                            navigate("/questions", { replace: true });
                            return;
                        }
                    }

                    //console.log("Auth0 user fully onboarded, showing home");
                    setLoading(false);
                }
                // Local JWT login
                else if (localToken) {
                    //console.log("Authenticated via local JWT");

                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });

                    if (!res.ok) throw new Error("Failed to fetch user info");

                    const data = await res.json();
                    //console.log("User data from backend:", data);
                    setUser(data);

                    // vrijeme za kviz
                    if (!data.isOnboardingComplete) {
                        if (!hasNavigatedToQuestions.current) {
                            //console.log("Onboarding not complete, redirecting to questions");
                            hasNavigatedToQuestions.current = true;
                            navigate("/questions", { replace: true });
                            return;
                        }
                    }

                    //console.log("Local user fully set up, showing home");
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

    const sendUserDataToBackend = async (auth0User) => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: `${AUDIENCE}`,
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
            //console.log("User data synced successfully:", userData);
            return userData;
        } catch (err) {
            console.error("Error sending user data to backend:", err);
            return null;
        }
    };

    const handleLogout = () => {
        try {
            localStorage.removeItem("token");

            if (isAuthenticated) {
                import("@auth0/auth0-react").then(({ useAuth0 }) => {
                    window.location.href = `${window.location.origin}/login`;
                });
            } else {
                navigate("/login");
            }
        } catch (err) {
            console.error("Logout error:", err);
            navigate("/login");
        }
    };

    function HomeLayout() {
        const [activeTab, setActiveTab] = useState('Personalized recomendations');

        const updateUser =(updatedFields) =>{
            setUser(prevUser =>({
                ...prevUser,
                ...updatedFields
            }));
        };

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
                
                case 'Settings':
                    return <Settings user={user} updateUser={updateUser}/>;
                

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

    return <HomeLayout />;
}
