import { useState, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import styles from './DailyMessages.module.css'; 

export default function DailyMessages() {
  const [messageData, setMessageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [timeOfDay, setTimeOfDay] = useState("AUTO");
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();
  
  const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
  const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

  useEffect(() => {
    fetchMessages();
  }, [timeOfDay]);

  const fetchMessages = async () => {
    setLoading(true);
    setError("");

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
        setError("Not authenticated");
        setLoading(false);
        return;
      }

      const response = await fetch(
        `${BACKEND_URL}/messages/me?timeOfDay=${timeOfDay}&count=3`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (response.status === 204) {
        setError("Please complete onboarding first.");
        setLoading(false);
        return;
      }

      const data = await response.json();
      setMessageData(data);
      setLoading(false);
    } catch (err) {
      setError("Failed to load messages.");
      setLoading(false);
    }
  };

  const getGoalLabel = (goal) => {
    const labels = {
      "REDUCE_ANXIETY": "Smanjenje anksioznosti",
      "IMPROVE_SLEEP": "Poboljšanje sna",
      "INCREASE_FOCUS": "Povećanje fokusa",
      "STRESS_MANAGEMENT": "Upravljanje stresom",
      "BUILD_HABIT": "Izgradnja navike"
    };
    return labels[goal] || goal;
  };

  if (loading) return <div className={styles.loadingContainer}>Učitavam...</div>;
  if (error) return <div className={styles.errorContainer}><p className={styles.errorText}>{error}</p></div>;

  return (
    <div className={styles.container}>
      <h2 className={styles.headerTitle}>
         Your daily message
      </h2>

      {messageData?.messages && messageData.messages.length > 0 ? (
        <div className={styles.scrollContainer}>
          {messageData.messages.map((item, index) => (
            <div key={index} className={styles.messageCard}>
              <div className={styles.goalBadgeContainer}>
                <span className={styles.goalBadge}>
                  {getGoalLabel(item.goal)}
                </span>
              </div>
              <p className={styles.messageText}>{item.text}</p>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.noMessagesContainer}>
          <p>Nema dostupnih poruka.</p>
        </div>
      )}

      <p className={styles.infoNote}>
        💡Poruke se personaliziraju na temelju tvojih odgovora u upitniku. 
        Mijenjaju se svaki dan i prilagođavaju se dijelu dana.
      </p>
    </div>
  );
}