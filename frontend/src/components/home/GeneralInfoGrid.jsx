import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import homeStyles from "../../pages/Home/Home.module.css";
import styles from "./GeneralInfoGrid.module.css";

import { useAuth0 } from "@auth0/auth0-react";

export default function GeneralInfoGrid() {
  const [recommendations, setRecommendations] = useState([]);
  const navigate = useNavigate();
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();
  const BACKEND_URL = process.env.REACT_APP_BACKEND;
  const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

  useEffect(() => {
    const fetchRecommendations = async () => {
      try {
        let token = localStorage.getItem("token");

        if (isAuthenticated) {
          token = await getAccessTokenSilently({
            authorizationParams: {
              audience: `${AUDIENCE}`,
              scope: "openid profile email",
            },
          });
        }

        if (!token) return;

        const res = await fetch(`${BACKEND_URL}/api/videos/recommendations`, {
          headers: {
            Authorization: `Bearer ${token}`
          }
        });

        if (res.ok) {
          const data = await res.json();
          setRecommendations(data);
        }
      } catch (err) {
        console.error("Error fetching recommendations:", err);
      }
    };

    fetchRecommendations();
  }, [BACKEND_URL, isAuthenticated, getAccessTokenSilently, AUDIENCE]);

  const handleCardClick = (item) => {
    navigate(`/watch/${item.id}`);
  };

  const getVideo = () => recommendations.find(r => r.type === 'VIDEO');
  const getBlog = () => recommendations.find(r => r.type === 'BLOG');
  const getAudio = () => recommendations.find(r => r.type === 'AUDIO');

  const renderRecommendationCard = (item, titleOverride) => {
    if (!item) return (
      <div className={homeStyles.card}>
        <h4>{titleOverride}</h4>
        <p>Loading...</p>
      </div>
    );

    return (
      <div className={homeStyles.card} onClick={() => handleCardClick(item)} style={{ cursor: 'pointer' }}>
        <h4>{titleOverride || item.title}</h4>
        <p className={styles.deepSleepText}>{item.type} • {item.duration} min</p>
        <div style={{ margin: '10px 0', fontSize: '14px' }}>
          {item.title}
        </div>
        <p className={styles.unlockedText}>
          By {item.trainerName}
        </p>
      </div>
    );
  };

  return (
    <div className={homeStyles.infoGrid}>
      {/* Video Recommendation */}
      {renderRecommendationCard(getVideo(), "Recommended Video")}

      {/* Article Recommendation */}
      {renderRecommendationCard(getBlog(), "Recommended Article")}

      {/* Podcast Recommendation */}
      {renderRecommendationCard(getAudio(), "Recommended Podcast")}

    </div>
  );
}