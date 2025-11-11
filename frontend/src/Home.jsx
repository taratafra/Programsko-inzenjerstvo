import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState } from "react";
import { useNavigate, useLocation} from "react-router-dom";
import styles from "./Home.module.css";

import Header from "./components/home/Header";
import LeftSidebar from "./components/home/LeftSidebar";
import RightSidebar from "./components/home/RightSidebar";
import DashboardTabs from "./components/home/DashboardTabs";
import GeneralInfoGrid from "./components/home/GeneralInfoGrid";

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
                setRequiresPasswordReset(false);
                alert("Password reset successfully! You can now use your new password.");
                setPasswordResetData({ newPassword: "", confirmPassword: "" });
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

    const handlePasswordChange = (e) => {
        const { name, value } = e.target;
        setPasswordResetData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const renderPasswordResetForm = () => (
        <div style={{
            border: "2px solid #f0ad4e",
            padding: "20px",
            borderRadius: "10px",
            backgroundColor: "#fcf8e3",
            margin: "20px 0"
        }}>
            <h3>First Login - Password Reset Required</h3>
            <p>Since this is your first login, you need to set a new password for your account.</p>

            <form onSubmit={handlePasswordReset}>
                <div style={{ marginBottom: "15px" }}>
                    <label style={{ display: "block", marginBottom: "5px" }}>
                        New Password:
                    </label>
                    <input
                        type="password"
                        name="newPassword"
                        value={passwordResetData.newPassword}
                        onChange={handlePasswordChange}
                        required
                        minLength="8"
                        style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #ccc" }}
                        placeholder="Enter new password (min 8 characters)"
                    />
                </div>

                <div style={{ marginBottom: "15px" }}>
                    <label style={{ display: "block", marginBottom: "5px" }}>
                        Confirm Password:
                    </label>
                    <input
                        type="password"
                        name="confirmPassword"
                        value={passwordResetData.confirmPassword}
                        onChange={handlePasswordChange}
                        required
                        minLength="8"
                        style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #ccc" }}
                        placeholder="Confirm new password"
                    />
                </div>

                <button
                    type="submit"
                    style={{
                        backgroundColor: "#5cb85c",
                        color: "white",
                        padding: "10px 20px",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer"
                    }}
                >
                    Reset Password
                </button>
            </form>
        </div>
    );

    const renderLoginStatus = () => {
        if (auth0User) {
            return <p style={{ color: "#5bc0de", fontStyle: "italic" }}>OAuth Login (No password reset required)</p>;
        } else if (user && !requiresPasswordReset) {
            return <p style={{ color: "#5cb85c", fontStyle: "italic" }}>Not first login - Regular local account</p>;
        }
        return null;
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
                            //handleLogout={handleLogout}
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
        <div style={{ padding: "20px" }}>
            {/* <h1>Login Successful!</h1>
            <h2>Welcome, {user.name || user.given_name || user.email}!</h2>
            <p>Email: {user.email}</p> */}

            {/* {renderLoginStatus()} */}

            {requiresPasswordReset && renderPasswordResetForm()}

            {/* {responseFromServer && (
                <div style={{ marginTop: "20px" }}>
                    <h3>Protected Resource Response:</h3>
                    <p>{responseFromServer}</p>
                </div>
            )} */}

            {!requiresPasswordReset && <HomeLayout />}

        </div>
    );
}