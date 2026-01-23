import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./MoodHabits.module.css";

export default function DailyExerciseVideo() {
    const [video, setVideo] = useState(null);
    const [loading, setLoading] = useState(true);

    const navigate = useNavigate();
    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE
    const { isAuthenticated, getAccessTokenSilently } = useAuth0();

    // 🔁 Deterministički daily index (isti video cijeli dan)
    const getDailyIndex = (length) => {
        if (!length) return 0;

        const today = new Date();
        const daySeed = Math.floor(
            Date.UTC(
                today.getFullYear(),
                today.getMonth(),
                today.getDate()
            ) / 86400000
        );

        return daySeed % length;
    };

    const fetchDailyVideo = useCallback(async () => {
        try {
            setLoading(true);

            const queryParams = new URLSearchParams();
            queryParams.append("type", "VIDEO");
            queryParams.append("goal", "BUILD_HABIT");
            queryParams.append("page", 0);
            queryParams.append("size", 100); // uzmi sve Build Habit videe

            const res = await fetch(
                `${BACKEND_URL}/api/videos?${queryParams.toString()}`
            );

            if (!res.ok) {
                console.error("Failed to fetch Build Habit videos");
                return;
            }

            const data = await res.json();
            const videos = data.content || [];

            if (videos.length === 0) {
                setVideo(null);
                return;
            }

            const dailyIndex = getDailyIndex(videos.length);
            setVideo(videos[dailyIndex]);
        } catch (err) {
            console.error("Error fetching daily Build Habit video:", err);
        } finally {
            setLoading(false);
        }
    }, [BACKEND_URL]);

    useEffect(() => {
        fetchDailyVideo();
    }, [fetchDailyVideo]);

    if (loading) return <div>Loading daily video...</div>;

    if (!video) {
        return (
            <div className={styles.mainContent}>
                <h2>No Build Habit videos available</h2>
            </div>
        );
    }

    return (
        <div className={styles.mainContent}>
            <h1>Daily Exercise Video</h1>

            <div className={styles.videoGrid}>
                <div className={styles.videoCard}>
                    <div
                        className={styles.videoThumbnail}
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                            navigate(`/watch/${video.id}`, {
                                state: { type: "VIDEO" }
                            })
                        }
                        onMouseEnter={(e) => {
                            const v = e.currentTarget.querySelector("video");
                            if (v) v.play().catch(() => {});
                        }}
                        onMouseLeave={(e) => {
                            const v = e.currentTarget.querySelector("video");
                            if (v) {
                                v.pause();
                                v.currentTime = 0;
                            }
                        }}
                    >
                        <video
                            src={video.url}
                            muted
                            width="100%"
                            height="100%"
                            style={{ objectFit: "cover", pointerEvents: "none" }}
                        />

                        <span className={styles.typeBadge}>VIDEO</span>
                    </div>

                    <div className={styles.videoInfo}>
                        <div
                            className={styles.videoTitle}
                            style={{ cursor: "pointer" }}
                            onClick={() =>
                                navigate(`/watch/${video.id}`, {
                                    state: { type: "VIDEO" }
                                })
                            }
                        >
                            {video.title}
                        </div>

                        <div className={styles.videoDescription}>
                            {video.description}
                        </div>

                        <div style={{ fontSize: "0.8rem", color: "#888" }}>
                            VIDEO • By {video.trainerName}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
