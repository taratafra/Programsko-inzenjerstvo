import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Videos.module.css";
// import { storage } from "../../utils/firebase";

export default function Videos({ contentType = "VIDEO" }) {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
    const [user, setUser] = useState(null);
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isFetchingMore, setIsFetchingMore] = useState(false);

    // --- UPLOAD STATES ---
    const [showUploadModal, setShowUploadModal] = useState(false);
    const [newVideo, setNewVideo] = useState({ title: "", description: "" });
    const [file, setFile] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isUploading, setIsUploading] = useState(false);

    // --- FILTER STATES ---
    const [filters, setFilters] = useState({
        goal: "",
        level: "",
        durationRange: ""
    });

    const navigate = useNavigate();
    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE

    // --- HELPER: FIXED DETECTION LOGIC ---
    const getMediaType = (item) => {
        if (item.type) return item.type;
        if (!item.url) return "VIDEO";
        const urlLower = item.url.toLowerCase();
        if (urlLower.includes('.mp3') || urlLower.includes('.wav') || urlLower.includes('.ogg') || urlLower.includes('audio')) return "AUDIO";
        if (urlLower.includes('.txt') || urlLower.includes('.md') || urlLower.includes('.pdf') || urlLower.includes('document')) return "BLOG";
        return "VIDEO";
    };

    const fetchVideos = useCallback(async (pageNum = 0, shouldReset = false) => {
        try {
            const queryParams = new URLSearchParams();
            queryParams.append("type", contentType);
            if (filters.goal) queryParams.append("goal", filters.goal);
            if (filters.level) queryParams.append("level", filters.level);
            if (filters.durationRange) queryParams.append("durationRange", filters.durationRange);
            queryParams.append("page", pageNum);
            queryParams.append("size", 10);

            if (pageNum > 0) setIsFetchingMore(true);

            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                authorizationParams: { audience: `${AUDIENCE}`, scope: "openid profile email" },
            });
        }

            const res = await fetch(`${BACKEND_URL}/api/videos?${queryParams.toString()}`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {},
            });
            if (res.ok) {
                const data = await res.json();
                // data is now a Page object: { content: [], number: 0, totalPages: 1, ... }
                const newVideos = data.content || [];

                setVideos(prev => shouldReset ? newVideos : [...prev, ...newVideos]);
                setHasMore(!data.last);
            }
        } catch (err) {
            console.error("Error fetching content:", err);
        } finally {
            setIsFetchingMore(false);
        }
    }, [BACKEND_URL, filters, contentType]);

    useEffect(() => {
        setPage(0);
        setHasMore(true);
        fetchVideos(0, true);
    }, [filters, contentType, fetchVideos]);

    const observer = useRef();
    const lastVideoElementRef = useCallback(node => {
        if (loading || isFetchingMore) return;
        if (observer.current) observer.current.disconnect();
        observer.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                setPage(prevPage => {
                    const nextPage = prevPage + 1;
                    fetchVideos(nextPage, false);
                    return nextPage;
                });
            }
        });
        if (node) observer.current.observe(node);
    }, [loading, hasMore, isFetchingMore, fetchVideos]);

    useEffect(() => {
        const init = async () => {
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
                        console.error("Error getting token or fetching user with Auth0:", tokenErr);
                    }
                    setLoading(false);
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
                        setLoading(false);
                    } else {
                        throw new Error("Failed to fetch user with local token");
                    }
                } else {
                    setLoading(false);
                }
            } catch (err) {
                console.error("Error initializing user:", err);
                if (!isAuthenticated) {
                    if (localToken) {
                        localStorage.removeItem("token");
                    }
                    navigate("/login");
                }
                setLoading(false);
            } finally {
                setLoading(false);
            }
        };

        if (!isLoading) {
            init();
            // fetchVideos is called by the filter effect
        }
    }, [isLoading, isAuthenticated, BACKEND_URL, AUDIENCE, getAccessTokenSilently, auth0User, navigate]);

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (!selectedFile) return;

        setFile(selectedFile);

        // Extract duration if it's video or audio
        if (selectedFile.type.startsWith('video/') || selectedFile.type.startsWith('audio/')) {
            const media = document.createElement(selectedFile.type.startsWith('video/') ? 'video' : 'audio');
            media.preload = 'metadata';
            media.onloadedmetadata = () => {
                window.URL.revokeObjectURL(media.src);
                // Round to nearest minute, minimum 1
                const durationInMinutes = Math.max(1, Math.round(media.duration / 60));
                setNewVideo(prev => ({ ...prev, duration: durationInMinutes }));
            };
            media.src = URL.createObjectURL(selectedFile);
        } else if (selectedFile.name.toLowerCase().endsWith('.md')) {
            // Word count for markdown articles
            const reader = new FileReader();
            reader.onload = (e) => {
                const text = e.target.result;
                // Basic word count: split by whitespace and filter out empty strings
                const words = text.trim().split(/\s+/).filter(word => word.length > 0);
                const wordCount = words.length;

                // Average reading speed: 200 words per minute
                const readingTime = Math.max(1, Math.ceil(wordCount / 200));
                setNewVideo(prev => ({ ...prev, duration: readingTime }));
            };
            reader.onerror = () => {
                console.error("Error reading markdown file");
                setNewVideo(prev => ({ ...prev, duration: "" }));
            };
            reader.readAsText(selectedFile);
        } else {
            setNewVideo(prev => ({ ...prev, duration: "" }));
        }
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) return alert("Please select a file first.");

        setIsUploading(true);
        setUploadProgress(10);

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: { audience: `${BACKEND_URL}`, scope: "openid profile email" },
                });
            }

            const formData = new FormData();
            formData.append("title", newVideo.title);
            formData.append("description", newVideo.description);
            formData.append("type", contentType);
            if (newVideo.goal) formData.append("goal", newVideo.goal);
            if (newVideo.level) formData.append("level", newVideo.level);
            if (newVideo.duration) formData.append("duration", newVideo.duration);
            formData.append("file", file);

            const progressInterval = setInterval(() => {
                setUploadProgress(prev => (prev >= 90 ? prev : prev + 10));
            }, 500);

            const res = await fetch(`${BACKEND_URL}/api/videos`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
                body: formData,
            });

            clearInterval(progressInterval);
            setUploadProgress(100);

            if (res.ok) {
                setShowUploadModal(false);
                setNewVideo({ title: "", description: "", goal: "", level: "", duration: "" });
                setFile(null);
                setUploadProgress(0);
                fetchVideos();
                alert("Upload successful!");
            } else {
                alert("Failed to upload: " + await res.text());
            }
        } catch (err) {
            console.error("Error uploading:", err);
            alert("Error uploading file");
        } finally {
            setIsUploading(false);
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("token");
        if (isAuthenticated) {
            import("@auth0/auth0-react").then(({ useAuth0 }) => {
                window.location.href = `${window.location.origin}/login`;
            });
        } else {
            navigate("/login");
        }
    };

    const getAcceptedFileTypes = () => {
        if (contentType === "AUDIO") return "audio/*";
        if (contentType === "BLOG") return ".md";
        return "video/*";
    };

    if (loading || isLoading) return <div>Loading...</div>;

    const hasLocalToken = !!localStorage.getItem("token");
    if (!user && !isAuthenticated && !hasLocalToken) {
        navigate("/login");
        return null;
    }

    if (!user) return <div>Loading user data...</div>;

    return (
        <>
            <div className={styles.mainContent}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                    <h1>{contentType === 'VIDEO' ? 'Videos' : contentType === 'AUDIO' ? 'Podcasts' : 'Articles'}</h1>
                    {user?.role === 'TRAINER' && (
                        <button className={styles.uploadButton} onClick={() => setShowUploadModal(true)}>
                            Upload New {contentType === 'VIDEO' ? 'Video' : contentType === 'AUDIO' ? 'Podcast' : 'Article'}
                        </button>
                    )}
                </div>

                {/* --- FILTERS --- */}
                <div className={styles.filterContainer}>
                    <div className={styles.filterGroup}>
                        <label>Goal</label>
                        <select
                            value={filters.goal}
                            onChange={(e) => setFilters({ ...filters, goal: e.target.value })}
                            className={styles.filterSelect}
                        >
                            <option value="">All Goals</option>
                            <option value="REDUCE_ANXIETY">Reduce Anxiety</option>
                            <option value="IMPROVE_SLEEP">Improve Sleep</option>
                            <option value="INCREASE_FOCUS">Increase Focus</option>
                            <option value="STRESS_MANAGEMENT">Stress Management</option>
                            <option value="BUILD_HABIT">Build Habit</option>
                        </select>
                    </div>

                    <div className={styles.filterGroup}>
                        <label>Level</label>
                        <select
                            value={filters.level}
                            onChange={(e) => setFilters({ ...filters, level: e.target.value })}
                            className={styles.filterSelect}
                        >
                            <option value="">All Levels</option>
                            <option value="BEGINNER">Beginner</option>
                            <option value="INTERMEDIATE">Intermediate</option>
                            <option value="ADVANCED">Advanced</option>
                        </select>
                    </div>

                    <div className={styles.filterGroup}>
                        <label>Duration</label>
                        <select
                            value={filters.durationRange}
                            onChange={(e) => setFilters({ ...filters, durationRange: e.target.value })}
                            className={styles.filterSelect}
                        >
                            <option value="">Any Length</option>
                            {contentType === 'AUDIO' ? (
                                <>
                                    <option value="short">Short (&lt; 1 hour)</option>
                                    <option value="long">Long (1–3 hours)</option>
                                    <option value="superlong">Superlong (&gt; 3 hours)</option>
                                </>
                            ) : (
                                <>
                                    <option value="5-10">5–10 minutes</option>
                                    <option value="10-15">10–15 minutes</option>
                                    <option value="15-20">15–20 minutes</option>
                                    <option value="20-plus">20+ minutes</option>
                                </>
                            )}
                        </select>
                    </div>

                    {(filters.goal || filters.level || filters.durationRange) && (
                        <button
                            className={styles.clearFilters}
                            onClick={() => setFilters({ goal: "", level: "", durationRange: "" })}
                        >
                            Clear Filters
                        </button>
                    )}
                </div>

                <div className={styles.videoGrid}>
                    {videos.map((item, index) => {
                        const itemType = getMediaType(item);
                        const isLastElement = videos.length === index + 1;

                        return (
                            <div
                                key={item.id}
                                className={styles.videoCard}
                                ref={isLastElement ? lastVideoElementRef : null}
                            >
                                <div
                                    className={styles.videoThumbnail}
                                    style={{ cursor: "pointer" }}
                                    onClick={() => navigate(`/watch/${item.id}`, { state: { type: itemType } })}
                                    onMouseEnter={(e) => {
                                        if (itemType === 'VIDEO') {
                                            const v = e.currentTarget.querySelector("video");
                                            if (v) v.play().catch(() => { });
                                        }
                                    }}
                                    onMouseLeave={(e) => {
                                        if (itemType === 'VIDEO') {
                                            const v = e.currentTarget.querySelector("video");
                                            if (v) { v.pause(); v.currentTime = 0; }
                                        }
                                    }}
                                >
                                    {/* RENDER CONTENT BASED ON TYPE */}
                                    {itemType === 'VIDEO' && (
                                        <video src={item.url} muted width="100%" height="100%" style={{ objectFit: 'cover', pointerEvents: 'none' }} />
                                    )}

                                    {itemType === 'AUDIO' && (
                                        <div className={`${styles.placeholderIcon} ${styles.audioIcon}`}>
                                            🎧
                                        </div>
                                    )}

                                    {itemType === 'BLOG' && (
                                        <div className={`${styles.placeholderIcon} ${styles.blogIcon}`}>
                                            📄
                                        </div>
                                    )}

                                    {/* Type Badge */}
                                    <span className={styles.typeBadge}>
                                        {itemType}
                                    </span>
                                </div>

                                <div className={styles.videoInfo}>
                                    <div
                                        className={styles.videoTitle}
                                        style={{ cursor: "pointer" }}
                                        onClick={() => navigate(`/watch/${item.id}`, { state: { type: itemType } })}
                                    >
                                        {item.title}
                                    </div>
                                    <div className={styles.videoDescription}>{item.description}</div>
                                    <div style={{ fontSize: '0.8rem', color: '#888' }}>
                                        {itemType} • By {item.trainerName}
                                    </div>
                                </div>
                            </div>
                        );
                    })}

                    {isFetchingMore && <div style={{ width: '100%', textAlign: 'center', padding: '20px' }}>Loading more...</div>}
                </div>
            </div>

            {showUploadModal && (
                <div className={styles.modalOverlay}>
                    <div className={styles.modalContent}>
                        <h2>Upload {contentType === 'VIDEO' ? 'Video' : contentType === 'AUDIO' ? 'Podcast' : 'Article'}</h2>
                        <form onSubmit={handleUpload}>
                            <div className={styles.formGroup}>
                                <label>Title</label>
                                <input type="text" value={newVideo.title} onChange={(e) => setNewVideo({ ...newVideo, title: e.target.value })} required />
                            </div>
                            <div className={styles.formGroup}>
                                <label>Description</label>
                                <textarea value={newVideo.description} onChange={(e) => setNewVideo({ ...newVideo, description: e.target.value })} required />
                            </div>

                            <div className={styles.formRow}>
                                <div className={styles.formGroup}>
                                    <label>Goal</label>
                                    <select
                                        value={newVideo.goal}
                                        onChange={(e) => setNewVideo({ ...newVideo, goal: e.target.value })}
                                        className={styles.selectInput}
                                    >
                                        <option value="">Select Goal</option>
                                        <option value="REDUCE_ANXIETY">Reduce Anxiety</option>
                                        <option value="IMPROVE_SLEEP">Improve Sleep</option>
                                        <option value="INCREASE_FOCUS">Increase Focus</option>
                                        <option value="STRESS_MANAGEMENT">Stress Management</option>
                                        <option value="BUILD_HABIT">Build Habit</option>
                                    </select>
                                </div>
                                <div className={styles.formGroup}>
                                    <label>Level</label>
                                    <select
                                        value={newVideo.level}
                                        onChange={(e) => setNewVideo({ ...newVideo, level: e.target.value })}
                                        className={styles.selectInput}
                                    >
                                        <option value="">Select Level</option>
                                        <option value="BEGINNER">Beginner</option>
                                        <option value="INTERMEDIATE">Intermediate</option>
                                        <option value="ADVANCED">Advanced</option>
                                    </select>
                                </div>
                            </div>

                            {newVideo.duration && (
                                <div className={styles.formGroup}>
                                    <p style={{ fontSize: '0.9rem', color: '#4caf50', margin: '10px 0' }}>
                                        ⏱️ Estimated {contentType === 'BLOG' ? 'reading time' : 'duration'}: <strong>
                                            {contentType === 'AUDIO' && newVideo.duration >= 60
                                                ? `${Math.floor(newVideo.duration / 60)}h ${newVideo.duration % 60}m`
                                                : `${newVideo.duration} min`}
                                        </strong>
                                    </p>
                                </div>
                            )}
                            <div className={styles.formGroup}>
                                <label>File</label>
                                <input type="file" accept={getAcceptedFileTypes()} onChange={handleFileChange} required />
                                {contentType === 'BLOG' && (
                                    <p style={{ fontSize: '0.75rem', color: '#888', marginTop: '5px' }}>
                                        * Only .md files are supported for articles.
                                    </p>
                                )}
                            </div>

                            {isUploading && (
                                <div className={styles.progressContainer}>
                                    <div className={styles.progressFill} style={{ width: `${uploadProgress}%` }} />
                                </div>
                            )}

                            <div className={styles.modalActions}>
                                <button type="button" className={styles.cancelButton} onClick={() => setShowUploadModal(false)}>Cancel</button>
                                <button type="submit" className={styles.submitButton} disabled={isUploading}>{isUploading ? 'Uploading...' : 'Upload'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </>
    );
}
