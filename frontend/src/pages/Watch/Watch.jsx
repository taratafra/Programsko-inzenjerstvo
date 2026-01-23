import React, { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./Watch.module.css";
import ReactMarkdown from "react-markdown";

// --- STAR RATING ---
const StarRating = ({ onRate, currentRating = 0, averageRating = 0, totalRatings = 0 }) => {
    const [rating, setRating] = useState(currentRating);
    const [hover, setHover] = useState(0);

    useEffect(() => {
        setRating(currentRating);
    }, [currentRating]);

    const handleRate = (value) => {
        setRating(value);
        if (onRate) onRate(value);
    };

    return (
        <div className={styles.starRating}>
            <div className={styles.starContainer}>
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
            </div>
            <div className={styles.ratingInfo}>
                {rating > 0 ? (
                    <span className={styles.ratingText}>Your rating: {rating}/5</span>
                ) : (
                    <span className={styles.ratingText}>Click to rate</span>
                )}
                {totalRatings > 0 && (
                    <span className={styles.averageRating}>
                        Average: {averageRating.toFixed(1)}/5 ({totalRatings} {totalRatings === 1 ? 'rating' : 'ratings'})
                    </span>
                )}
            </div>
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
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [replyingTo, setReplyingTo] = useState(null);
    const [replyText, setReplyText] = useState("");

    // --- SUBSCRIBE STATE ---
    const [isSubscribed, setIsSubscribed] = useState(false);
    const [isSubscribing, setIsSubscribing] = useState(false);

    // --- RATING STATE ---
    const [userRating, setUserRating] = useState(0);
    const [averageRating, setAverageRating] = useState(0);
    const [totalRatings, setTotalRatings] = useState(0);
    const [isRating, setIsRating] = useState(false);

    // --- CONTENT DATA STATE ---
    const [contentData, setContentData] = useState({
        title: "Loading...",
        type: location.state?.type || "VIDEO",
        url: "",
        textBody: "",
        trainerId: null
    });

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE
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
                let token = localStorage.getItem("token");

                if (isAuthenticated) {
                    token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                    });
                }

                const res = await fetch(`${BACKEND_URL}/api/videos/${id}`, {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                });
                //const res = await fetch(`${BACKEND_URL}/api/videos/${id}`);
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
                        trainerId: data.trainerId,
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

                    // Check if already subscribed
                    if (data.trainerId) {
                        checkSubscriptionStatus(data.trainerId);
                    }
                }
            } catch (err) {
                console.error("Error fetching content:", err);
            } finally {
                setLoading(false);
            }
        };

        fetchContent();
        fetchComments();
        fetchRatings();
    }, [id, location.state?.type, BACKEND_URL]);

    const fetchRatings = async () => {
        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                try {
                    token = await getAccessTokenSilently({
                        authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                    });
                } catch (e) {
                    console.error("Error getting token for ratings:", e);
                }
            }

            const headers = token ? { Authorization: `Bearer ${token}` } : {};
            const res = await fetch(`${BACKEND_URL}/api/videos/${id}/ratings`, { headers });
            
            if (res.ok) {
                const data = await res.json();
                setAverageRating(data.averageRating || 0);
                setTotalRatings(data.totalRatings || 0);
                setUserRating(data.userRating || 0);
            }
        } catch (err) {
            console.error("Error fetching ratings:", err);
        }
    };

    const handleRateVideo = async (ratingValue) => {
        if (isRating) return;
        setIsRating(true);

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                });
            }

            if (!token) {
                alert("Please log in to rate this content");
                setIsRating(false);
                return;
            }

            const res = await fetch(`${BACKEND_URL}/api/videos/${id}/ratings`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify({ rating: ratingValue })
            });

            if (res.ok) {
                const data = await res.json();
                setUserRating(data.userRating);
                setAverageRating(data.averageRating);
                setTotalRatings(data.totalRatings);
            } else {
                alert("Failed to submit rating");
            }
        } catch (err) {
            console.error("Error submitting rating:", err);
            alert("Error submitting rating");
        } finally {
            setIsRating(false);
        }
    };

    const checkSubscriptionStatus = async (trainerId) => {
        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                try {
                    token = await getAccessTokenSilently({
                        authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                    });
                } catch (e) {
                    console.error("Error getting token:", e);
                    return;
                }
            }

            if (!token) return;

            // Use the correct endpoint that returns list of trainer IDs
            const res = await fetch(`${BACKEND_URL}/api/trainers/me/subscriptions`, {
                headers: { Authorization: `Bearer ${token}` }
            });

            if (res.ok) {
                const trainerIds = await res.json(); // This is an array of trainer IDs
                console.log('📋 User subscriptions (trainer IDs):', trainerIds);
                console.log('🎯 Checking if subscribed to trainer:', trainerId);
                
                // Check if the current trainer's ID is in the list
                const isCurrentlySubscribed = trainerIds.includes(trainerId);
                console.log('✅ Is subscribed?', isCurrentlySubscribed);
                
                setIsSubscribed(isCurrentlySubscribed);
            }
        } catch (err) {
            console.error("Error checking subscription status:", err);
        }
    };

    const handleSubscribe = async () => {
        if (!contentData.trainerId) {
            alert("Trainer information not available");
            return;
        }

        setIsSubscribing(true);

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                });
            }

            if (!token) {
                alert("Please log in to subscribe");
                setIsSubscribing(false);
                return;
            }

            let res;
            if (isSubscribed) {
                // Unsubscribe - DELETE request
                res = await fetch(`${BACKEND_URL}/api/trainers/me/${contentData.trainerId}`, {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                });
            } else {
                // Subscribe - POST request
                res = await fetch(`${BACKEND_URL}/api/trainers/me/subscribe`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`
                    },
                    body: JSON.stringify({ trainerId: contentData.trainerId })
                });
            }

            if (res.ok || res.status === 204) {
                // Toggle the subscription state
                const newState = !isSubscribed;
                setIsSubscribed(newState);
                alert(newState ? "Subscribed successfully!" : "Unsubscribed successfully!");
            } else {
                const errorText = await res.text();
                alert(`Failed to ${isSubscribed ? 'unsubscribe' : 'subscribe'}: ${errorText}`);
            }
        } catch (err) {
            console.error("Error processing subscription:", err);
            alert("Error processing subscription");
        } finally {
            setIsSubscribing(false);
        }
    };

    const fetchComments = async () => {
        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                try {
                    token = await getAccessTokenSilently({
                        authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                    });
                } catch (e) {
                    console.error("Error getting token for comments:", e);
                }
            }

            const headers = token ? { Authorization: `Bearer ${token}` } : {};
            const res = await fetch(`${BACKEND_URL}/api/videos/${id}/comments`, { headers });
            if (res.ok) {
                const data = await res.json();
                setComments(data);
            }
        } catch (err) {
            console.error("Error fetching comments:", err);
        }
    };

    const handlePostComment = async () => {
        if (!newComment.trim()) return;

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                });
            }

            const res = await fetch(`${BACKEND_URL}/api/videos/${id}/comments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify({ text: newComment })
            });

            if (res.ok) {
                const savedComment = await res.json();
                setComments([savedComment, ...comments]);
                setNewComment("");
            }
        } catch (err) {
            console.error("Error posting comment:", err);
        }
    };

    const handleReply = async (parentId) => {
        if (!replyText.trim()) return;

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                });
            }

            const res = await fetch(`${BACKEND_URL}/api/comments/${parentId}/replies`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify({ text: replyText })
            });

            if (res.ok) {
                await fetchComments();
                setReplyingTo(null);
                setReplyText("");
            }
        } catch (err) {
            console.error("Error posting reply:", err);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!window.confirm("Are you sure you want to delete this comment?")) return;

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
                });
            }

            const res = await fetch(`${BACKEND_URL}/api/comments/${commentId}`, {
                method: "DELETE",
                headers: { Authorization: `Bearer ${token}` }
            });

            if (res.ok) {
                fetchComments();
            }
        } catch (err) {
            console.error("Error deleting comment:", err);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') handlePostComment();
    };

    const CommentItem = ({ comment, depth = 0 }) => (
        <div className={styles.comment} style={{ marginLeft: depth > 0 ? '20px' : '0', borderLeft: depth > 0 ? '2px solid #eee' : 'none', paddingLeft: depth > 0 ? '10px' : '0' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {comment.authorPicture ? (
                        <img src={comment.authorPicture} alt="u" className={styles.avatarImage} style={{ width: '24px', height: '24px' }} />
                    ) : (
                        <div className={styles.userAvatar} style={{ width: '24px', height: '24px', fontSize: '12px' }}>👤</div>
                    )}
                    <strong style={{ color: '#2b3674' }}>{comment.authorName}</strong>
                </div>
                <span style={{ fontSize: '0.8rem', color: '#999' }}>{new Date(comment.createdAt).toLocaleString()}</span>
            </div>
            <div style={{ marginBottom: '5px' }}>{comment.text}</div>

            <div style={{ display: 'flex', gap: '10px', fontSize: '0.9rem' }}>
                <button
                    onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
                    style={{ background: 'none', border: 'none', color: '#4318FF', cursor: 'pointer', padding: 0 }}
                >
                    Reply
                </button>
                {comment.isOwner && (
                    <button
                        onClick={() => handleDeleteComment(comment.id)}
                        className={styles.deleteButton}
                    >
                        Delete
                    </button>
                )}
            </div>

            {replyingTo === comment.id && (
                <div style={{ marginTop: '10px', display: 'flex', gap: '10px' }}>
                    <input
                        type="text"
                        placeholder="Write a reply..."
                        className={styles.commentInput}
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        autoFocus
                    />
                    <button className={styles.postBtn} onClick={() => handleReply(comment.id)}>Reply</button>
                </div>
            )}

            {comment.replies && comment.replies.length > 0 && (
                <div style={{ marginTop: '10px' }}>
                    {comment.replies.map(reply => (
                        <CommentItem key={reply.id} comment={reply} depth={depth + 1} />
                    ))}
                </div>
            )}
        </div>
    );

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
                                    <ReactMarkdown>{contentData.textBody}</ReactMarkdown>
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
                                <button 
                                    className={styles.subscribeBtn}
                                    onClick={handleSubscribe}
                                    disabled={isSubscribing}
                                    style={{
                                        backgroundColor: isSubscribed ? '#ff6b6b' : '#4318FF',
                                        cursor: isSubscribing ? 'not-allowed' : 'pointer'
                                    }}
                                >
                                    {isSubscribing ? 'Processing...' : isSubscribed ? 'Unsubscribe' : 'Subscribe'}
                                </button>
                            </div>

                            {/* --- REVIEWS --- */}
                            <div className={styles.actionRow}>
                                <div className={styles.reviewSection}>
                                    <span>Leave a Review:</span>
                                    <StarRating 
                                        onRate={handleRateVideo} 
                                        currentRating={userRating}
                                        averageRating={averageRating}
                                        totalRatings={totalRatings}
                                    />
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
                                        <CommentItem key={comment.id} comment={comment} />
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
