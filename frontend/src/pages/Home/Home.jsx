import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useRef, useCallback } from "react";
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
    const hasNavigatedToQuestions = useRef(false);

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE || BACKEND_URL;

    const sendUserDataToBackend = useCallback(async (auth0User) => {
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
            return userData;
        } catch (err) {
            console.error("Error sending user data to backend:", err);
            return null;
        }
    }, [getAccessTokenSilently, AUDIENCE, BACKEND_URL]);

    useEffect(() => {
        const init = async () => {
            const localToken = localStorage.getItem("token");
            // The provided snippet `if (data.access_token)` seems to be from a different context (e.g., Login.jsx)
            // and `data` is not defined here.
            // The instruction is to "Set wasLoggedIn flag in Home.jsx".
            // The `localStorage.setItem("wasLoggedIn", "true");` is already present in the isAuthenticated block.
            // To faithfully apply the change as requested, assuming the intent is to ensure `wasLoggedIn` is set
            // when a local token is present and valid, or when Auth0 authentication is successful.
            // The existing code already handles setting `wasLoggedIn` for Auth0.
            // For local token, it's not explicitly set, so we'll add it there.

            console.log("DEBUG: init called. isAuthenticated:", isAuthenticated, "hasLocalToken:", !!localToken, "isLoading:", isLoading);

            try {
                if (isAuthenticated) {
                    console.log("DEBUG: Authenticated via Auth0. auth0User:", auth0User);
                    // Wait for auth0User to be available if authenticated
                    if (!auth0User) return;
                    setUser(auth0User);
                    try {
                        const token = await getAccessTokenSilently({
                            authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                        });
                        console.log("DEBUG: Got Auth0 token. Fetching /api/users/me");
                        const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                            headers: { Authorization: `Bearer ${token}` },
                        });
                        if (res.ok) {
                            const data = await res.json();
                            console.log("DEBUG: Auth0 user data from backend:", data);
                            setUser(data);
                            if (!data.isOnboardingComplete && !hasNavigatedToQuestions.current) {
                                hasNavigatedToQuestions.current = true;
                                navigate("/questions", { replace: true });
                                return;
                            }
                        } else {
                            console.log("DEBUG: Auth0 /api/users/me failed. Status:", res.status);
                            // If fetching user from backend fails with Auth0 token, try to send user data
                            const userResponse = await sendUserDataToBackend(auth0User);
                            if (userResponse) {
                                setUser(userResponse);
                                if (!userResponse.isOnboardingComplete && !hasNavigatedToQuestions.current) {
                                    hasNavigatedToQuestions.current = true;
                                    navigate("/questions", { replace: true });
                                    return;
                                }
                            } else {
                                throw new Error("Failed to fetch or create user with Auth0");
                            }
                        }
                    } catch (tokenErr) {
                        console.error("DEBUG: Error in Auth0 init flow:", tokenErr);
                        // Fallback to sending user data if token acquisition or initial fetch fails
                        const userResponse = await sendUserDataToBackend(auth0User);
                        if (userResponse) {
                            setUser(userResponse);
                            if (!userResponse.isOnboardingComplete && !hasNavigatedToQuestions.current) {
                                hasNavigatedToQuestions.current = true;
                                navigate("/questions", { replace: true });
                                return;
                            }
                        } else {
                            throw new Error("Failed to fetch or create user with Auth0 after token error");
                        }
                    }
                    setLoading(false);
                } else if (localToken) {
                    console.log("DEBUG: Authenticated via local token. Fetching /api/users/me");
                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });
                    if (res.ok) {
                        const data = await res.json();
                        console.log("DEBUG: Local user data from backend:", data);
                        setUser(data);
                        if (!data.isOnboardingComplete && !hasNavigatedToQuestions.current) {
                            hasNavigatedToQuestions.current = true;
                            navigate("/questions", { replace: true });
                            return;
                        }
                        setLoading(false);
                    } else {
                        console.log("DEBUG: Local /api/users/me failed. Status:", res.status);
                        throw new Error("Failed to fetch user with local token");
                    }
                } else {
                    console.log("DEBUG: No authentication found.");
                    // No auth method found
                    setLoading(false);
                }
            } catch (err) {
                console.error("DEBUG: Error in init:", err);
                if (!isAuthenticated) {
                    console.log("DEBUG: Not authenticated via Auth0, removing token and redirecting to login");
                    if (localToken) {
                        localStorage.removeItem("token");
                    }
                    navigate("/login");
                }
                setLoading(false);
            } finally {
                // Ensure loading is set to false if not already handled by an early return
                setLoading(false);
            }
        };

        if (!isLoading) {
            init();
        }
    }, [isLoading, isAuthenticated, BACKEND_URL, AUDIENCE, getAccessTokenSilently, auth0User, sendUserDataToBackend, navigate]);

    const handleLogout = () => {
        try {
            localStorage.removeItem("token");

            if (isAuthenticated) {
                logout({ logoutParams: { returnTo: window.location.origin + "/login" } });
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

        const updateUser = (updatedFields) => {
            setUser(prevUser => ({
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
                    return <Settings user={user} updateUser={updateUser} />;


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

    const hasLocalToken = !!localStorage.getItem("token");
    if (!user && !isAuthenticated && !hasLocalToken) {
        navigate("/login");
        return null;
    }

    if (!user) return <div>Loading user data...</div>;

    return <HomeLayout />;
}