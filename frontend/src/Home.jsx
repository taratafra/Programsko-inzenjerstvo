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
    const { user, getAccessTokenSilently, isLoading } = useAuth0();
    const [responseFromServer, setResponse] = useState('');
    const navigate = useNavigate();

    console.log("Home - Current user:", user);
    console.log("Home - isLoading:", isLoading);


    useEffect(() => {
        if (user) {
            console.log("User found, fetching protected data");
            sendUserDataToBackend();  
            getDataFromResourceServer();
        }
    }, [user]);

      const sendUserDataToBackend = async () => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: 'http://localhost:8080',
                    scope: "openid profile email",
                }
            });

            const payload = {
                name: user.given_name || user.name?.split(" ")[0] || "",
                surname: user.family_name || user.name?.split(" ")[1] || "",
                email: user.email,
                lastLogin: new Date().toISOString(),
                isSocialLogin: true,
                auth0Id: user.sub,
            };

            console.log("Sending user data:", payload);

            const response = await fetch("http://localhost:8080/api/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                console.error("Failed to send user data:", response.statusText);
            } else {
                console.log("User data sent successfully");
            }
        } catch (error) {
            console.error("Error sending user data to backend:", error);
        }
    };

    const getDataFromResourceServer = async () => {
        try {
            console.log("token");
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: 'http://localhost:8080',
                    scope: "openid profile email",
                }
            });
            const response = await fetch("http://localhost:8080/protected", {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
            const responseData = await response.text();
            console.log(responseData);
            setResponse(responseData);
        } catch (error) {
            console.error("Error fetching protected data:", error);
        }
    }


    if (isLoading) {
        return <div>Loading...</div>;
    }

    if (!user) {
        return <div>No user found...</div>;
    }

    console.log("Login Successful!");
    console.log(`Welcome, ${user.name}!`);
    console.log(responseFromServer);
    console.log(`Email: ${user.email}`);

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
    return (
        <div>
            <HomeLayout />
        </div>
    );
}