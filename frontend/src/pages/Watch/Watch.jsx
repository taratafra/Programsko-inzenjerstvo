import React, { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./Watch.module.css";

// --- STAR RATING ---
const StarRating = ({ onRate }) => {
    const [rating, setRating] = useState(0);
    const [hover, setHover] = useState(0);

    const handleRate = (value) => {
        setRating(value);
        if (onRate) onRate(value);
    };

    return (
        <div className={styles.starRating}>
            {[...Array(5)].map((_, index) => {
                const ratingValue = index + 1;
                return (
                    <span
                        key={index}
                        className={ratingValue <= (hover || rating) ? styles.starFilled : styles.starEmpty}
                        onClick={() => handleRate(ratingValue)}
                        onMouseEnter={() => setHover(ratingValue)}
                        onMouseLeave={() => setHover(0)}
                    >
                        ★
                    </span>
                );
            })}
            <span className={styles.ratingText}>
                {rating > 0 ? `You rated: ${rating}/5` : "Rate"}
            </span>
        </div>
    );
};

export default function Watch() {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    const { user: auth0User, isAuthenticated, isLoading } = useAuth0();
    const [user, setUser] = useState(null);

    // --- COMMENTS STATE ---
    const [comments, setComments] = useState([
        { id: 1, user: "Trainer Mike", text: "Great form on the second set!", date: "2 hours ago" },
        { id: 2, user: "Sarah J.", text: "Can you upload the PDF version of this?", date: "1 day ago" }
    ]);
    const [newComment, setNewComment] = useState("");

    // --- CONTENT DATA STATE ---
    const [contentData, setContentData] = useState({
        title: "Loading...",
        type: location.state?.type || "VIDEO",
        url: "",
        textBody: ""
    });

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE || BACKEND_URL;
    const [loading, setLoading] = useState(true);

    const { getAccessTokenSilently } = useAuth0();

    useEffect(() => {
        const initUser = async () => {
            const localToken = localStorage.getItem("token");
            try {
                if (isAuthenticated) {
                    if (!auth0User) return;
                    setUser(auth0User);
                    try {
                        const token = await getAccessTokenSilently({
                            authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                        });
                        const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                            headers: { Authorization: `Bearer ${token}` },
                        });
                        if (res.ok) {
                            const data = await res.json();
                            setUser(data);
                            if (!data.isOnboardingComplete) {
                                navigate("/questions", { replace: true });
                                return;
                            }
                        }
                    } catch (tokenErr) {
                        console.error("Error getting token in Watch:", tokenErr);
                    }
                    // We don't set loading false here because fetchContent handles it
                } else if (localToken) {
                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });
                    if (res.ok) {
                        const data = await res.json();
                        setUser(data);
                        if (!data.isOnboardingComplete) {
                            navigate("/questions", { replace: true });
                            return;
                        }
                    } else {
                        throw new Error("Failed to fetch user");
                    }
                }
            } catch (err) {
                console.error("Error initializing user in Watch:", err);
                if (!isAuthenticated) {
                    if (localToken) {
                        localStorage.removeItem("token");
                    }
                    navigate("/login");
                }
            }
        };
        initUser();
    }, [isAuthenticated, auth0User, BACKEND_URL, AUDIENCE, getAccessTokenSilently, navigate]);

    useEffect(() => {
        const fetchContent = async () => {
            try {
                const res = await fetch(`${BACKEND_URL}/api/videos/${id}`);
                if (res.ok) {
                    const data = await res.json();

                    // Use type from backend, fallback to state or inference
                    let itemType = data.type || location.state?.type;
                    if (!itemType) {
                        const urlLower = data.url.toLowerCase();
                        if (urlLower.includes('.mp3') || urlLower.includes('.wav') || urlLower.includes('.ogg') || urlLower.includes('audio')) itemType = "AUDIO";
                        else if (urlLower.includes('.txt') || urlLower.includes('.md') || urlLower.includes('.pdf') || urlLower.includes('document')) itemType = "BLOG";
                        else itemType = "VIDEO";
                    }

                    setContentData({
                        title: data.title,
                        description: data.description,
                        type: itemType,
                        url: data.url,
                        trainerName: data.trainerName,
                        createdAt: data.createdAt,
                        textBody: itemType === 'BLOG' ? "Loading blog content..." : ""
                    });

                    if (itemType === 'BLOG') {
                        // Fetch blog text if it's a blog
                        const textRes = await fetch(data.url);
                        if (textRes.ok) {
                            const text = await textRes.text();
                            setContentData(prev => ({ ...prev, textBody: text }));
                        }
                    }
                }
            } catch (err) {
                console.error("Error fetching content:", err);
            } finally {
                setLoading(false);
            }
        };

        fetchContent();
    }, [id, location.state?.type, BACKEND_URL]);

    const handlePostComment = () => {
        if (!newComment.trim()) return;

        const commentObject = {
            id: Date.now(),
            user: user?.name || "Guest User",
            text: newComment,
            date: "Just now"
        };

        setComments([commentObject, ...comments]);
        setNewComment("");
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') handlePostComment();
    };

    const handleDeleteComment = (commentId) => {
        setComments(comments.filter(c => c.id !== commentId));
    };

    if (loading || isLoading) return <div className={styles.layoutContainer}><div className={styles.centerContainer}>Loading content...</div></div>;

    const hasLocalToken = !!localStorage.getItem("token");
    if (!user && !isAuthenticated && !hasLocalToken) {
        navigate("/login");
        return null;
    }

    if (!user) return <div className={styles.layoutContainer}><div className={styles.centerContainer}>Loading user data...</div></div>;

    return (
        <div className={styles.layoutContainer}>
            <div className={styles.cloud1}></div>
            <div className={styles.cloud2}></div>
            <div className={styles.cloud3}></div>

            <div className={styles.contentWrapper}>
                <div className={styles.scrollableArea}>
                    <div className={styles.centerContainer}>

                        <button onClick={() => navigate(-1)} className={styles.backButton}>
                            ← Back to Content Library
                        </button>

                        <div className={styles.watchCard}>

                            {/* --- PLAYER SECTION --- */}
                            {contentData.type === 'VIDEO' && (
                                <div className={styles.videoPlayerWrapper}>
                                    <video controls autoPlay className={styles.videoPlayer} src={contentData.url}>
                                        Your browser does not support video.
                                    </video>
                                </div>
                            )}

                            {contentData.type === 'AUDIO' && (
                                <div className={styles.audioWrapper}>
                                    <div className={styles.audioIconLarge}>🎧</div>
                                    <audio controls src={contentData.url} className={styles.audioElement} />
                                </div>
                            )}

                            {contentData.type === 'BLOG' && (
                                <div className={styles.blogTextWrapper}>
                                    {contentData.textBody.split('\n').map((line, i) => (
                                        <p key={i} className={styles.blogParagraph}>{line}</p>
                                    ))}
                                </div>
                            )}

                            {/* --- INFO HEADER --- */}
                            <div className={styles.videoHeader}>
                                <div>
                                    <h1 className={styles.videoTitle}>{contentData.title}</h1>
                                    <p className={styles.videoDate}>
                                        By {contentData.trainerName} • {contentData.createdAt ? new Date(contentData.createdAt).toLocaleDateString() : "Recently"}
                                    </p>
                                </div>
                                <button className={styles.subscribeBtn}>Subscribe</button>
                            </div>

                            {/* --- REVIEWS --- */}
                            <div className={styles.actionRow}>
                                <div className={styles.reviewSection}>
                                    <span>Leave a Review:</span>
                                    <StarRating onRate={(val) => console.log(val)} />
                                </div>
                            </div>

                            {/* --- DESCRIPTION --- */}
                            <div className={styles.descriptionBox}>
                                <h3>Description</h3>
                                <p>{contentData.description || "No description provided."}</p>
                            </div>

                            {/* --- COMMENTS --- */}
                            <div className={styles.commentsSection}>
                                <h3>Comments ({comments.length})</h3>

                                <div className={styles.commentInputWrapper}>
                                    <div className={styles.userAvatar}>
                                        {user?.picture ?
                                            <img src={user.picture} alt="u" className={styles.avatarImage} />
                                            : "👤"
                                        }
                                    </div>
                                    <input
                                        type="text"
                                        placeholder="Add a comment..."
                                        className={styles.commentInput}
                                        value={newComment}
                                        onChange={(e) => setNewComment(e.target.value)}
                                        onKeyDown={handleKeyDown}
                                    />
                                    <button className={styles.postBtn} onClick={handlePostComment}>Post</button>
                                </div>

                                <div className={styles.commentList}>
                                    {comments.map((comment) => (
                                        <div key={comment.id} className={styles.comment}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                                                <strong style={{ color: '#2b3674' }}>{comment.user}</strong>
                                                <span style={{ fontSize: '0.8rem', color: '#999' }}>{comment.date}</span>
                                            </div>
                                            <div>{comment.text}</div>

                                            {comment.user === (user?.name || "Guest User") && (
                                                <button
                                                    onClick={() => handleDeleteComment(comment.id)}
                                                    className={styles.deleteButton}
                                                >
                                                    Delete
                                                </button>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}