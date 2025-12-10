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
            audience: BACKEND_URL,
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
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 204) {
        setError("Please complete the onboarding questionnaire first to receive personalized messages.");
        setLoading(false);
        return;
      }

      if (!response.ok) {
        throw new Error(`Failed to fetch messages: ${response.status}`);
      }

      const data = await response.json();
      console.log("Received messages:", data);
      setMessageData(data);
      setLoading(false);
    } catch (err) {
      console.error("Error fetching messages:", err);
      setError("Failed to load messages. Please try again.");
      setLoading(false);
    }
  };

  const getTimeOfDayEmoji = (tod) => {
    switch (tod) {
      case "MORNING": return "🌅";
      case "MIDDAY": return "☀️";
      case "EVENING": return "🌙";
      default: return "🕐";
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

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <p>Učitavam tvoje personalizirane poruke...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.errorContainer}>
        <p className={styles.errorText}>{error}</p>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      {/* Header with time selector */}
      <div className={styles.header}>
        <h2 className={styles.headerTitle}>
          {getTimeOfDayEmoji(messageData?.timeOfDay)} Your daily message
        </h2>
      </div>

      {/* Messages */}
      {messageData?.messages && messageData.messages.length > 0 ? (
        <div className={styles.scrollContainer}>
          {messageData.messages.map((item, index) => (
            <div key={index} className={styles.messageCard}>
              {/* Goal badge */}
              <div className={styles.goalBadgeContainer}>
                <span className={styles.goalBadge}>
                  {getGoalLabel(item.goal)}
                </span>
              </div>

              {/* Message text */}
              <p className={styles.messageText}>
                {item.text}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.noMessagesContainer}>
          <p>Nema dostupnih poruka. Molimo ispuni upitnik za početak.</p>
        </div>
      )}
      {/* Info note */}
      <p className={styles.infoNote}>
        💡Poruke se personaliziruju na temelju tvojih odgovora u upitniku.
        Mijenjaju se svaki dan i prilagođavaju se dijelu dana.
      </p>
    </div>
  );
}