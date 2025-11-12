// DailyMessages.jsx - Component to display personalized daily messages
import { useState, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";

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
      // Get token
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

      // Fetch messages - your API uses count parameter for number of goals
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
    switch(tod) {
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
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <p>Učitavam tvoje personalizirane poruke...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ 
        padding: "1.5rem", 
        backgroundColor: "#fff3cd", 
        border: "1px solid #ffc107", 
        borderRadius: "8px",
        margin: "1rem"
      }}>
        <p style={{ color: "#856404", margin: 0 }}>{error}</p>
      </div>
    );
  }

  return (
    <div style={{ padding: "2rem", maxWidth: "900px", margin: "0 auto" }}>
      {/* Header with time selector */}
      <div style={{ 
        display: "flex", 
        justifyContent: "space-between", 
        alignItems: "center",
        marginBottom: "2rem",
        flexWrap: "wrap",
        gap: "1rem"
      }}>
        <h2 style={{ margin: 0 }}>
          {getTimeOfDayEmoji(messageData?.timeOfDay)} Tvoje dnevne poruke
        </h2>
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <label htmlFor="timeOfDay" style={{ fontWeight: "500" }}>
            Dio dana:
          </label>
          <select
            id="timeOfDay"
            value={timeOfDay}
            onChange={(e) => setTimeOfDay(e.target.value)}
            style={{
              padding: "0.5rem 1rem",
              borderRadius: "6px",
              border: "1px solid #ddd",
              backgroundColor: "white",
              cursor: "pointer",
              fontSize: "0.95rem"
            }}
          >
            <option value="AUTO">Automatski (trenutno vrijeme)</option>
            <option value="MORNING">Jutro 🌅</option>
            <option value="MIDDAY">Dan ☀️</option>
            <option value="EVENING">Večer 🌙</option>
          </select>
        </div>
      </div>

      {/* Messages */}
      {messageData?.messages && messageData.messages.length > 0 ? (
        <div style={{ display: "flex", flexDirection: "row", gap: "1.5rem" }}>
          {messageData.messages.map((item, index) => (
            <div
              key={index}
              style={{
                padding: "1.5rem",
                backgroundColor: "white",
                border: "1px solid #e0e0e0",
                borderLeft: "4px solid #4a90e2",
                borderRadius: "8px",
                boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
                transition: "transform 0.2s, box-shadow 0.2s",
                cursor: "default"
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = "translateY(-2px)";
                e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.12)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = "translateY(0)";
                e.currentTarget.style.boxShadow = "0 2px 8px rgba(0,0,0,0.08)";
              }}
            >
              {/* Goal badge */}
              <div style={{ marginBottom: "0.75rem" }}>
                <span style={{
                  display: "inline-block",
                  padding: "0.25rem 0.75rem",
                  backgroundColor: "#e3f2fd",
                  color: "#1976d2",
                  borderRadius: "12px",
                  fontSize: "0.85rem",
                  fontWeight: "500"
                }}>
                  {getGoalLabel(item.goal)}
                </span>
              </div>
              
              {/* Message text */}
              <p style={{ 
                margin: 0, 
                lineHeight: "1.7",
                fontSize: "1.05rem",
                color: "#333"
              }}>
                {item.text}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <div style={{
          padding: "2rem",
          textAlign: "center",
          backgroundColor: "#f8f9fa",
          borderRadius: "8px"
        }}>
          <p>Nema dostupnih poruka. Molimo ispuni upitnik za početak.</p>
        </div>
      )}

      {/* Refresh button */}
      <button
        onClick={fetchMessages}
        style={{
          marginTop: "2rem",
          padding: "0.75rem 1.5rem",
          backgroundColor: "#4a90e2",
          color: "white",
          border: "none",
          borderRadius: "6px",
          cursor: "pointer",
          fontSize: "1rem",
          fontWeight: "500",
          transition: "background-color 0.2s",
          width: "100%"
        }}
        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = "#357abd"}
        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = "#4a90e2"}
      >
        🔄 Osvježi poruke
      </button>

      {/* Info note */}
      <p style={{
        marginTop: "1.5rem",
        fontSize: "0.9rem",
        color: "#666",
        textAlign: "center",
        fontStyle: "italic"
      }}>
        💡 Savjet: Poruke se personaliziraju na temelju tvojih odgovora u upitniku. 
        Mijenjaju se svaki dan i prilagođavaju se dijelu dana.
      </p>
    </div>
  );
}