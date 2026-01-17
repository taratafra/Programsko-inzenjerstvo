import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Videos.module.css";
<<<<<<<< HEAD:frontend/src/components/home/tabPanel/Videos/Videos.jsx
// import { storage } from "../../utils/firebase";

export default function Videos() {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
    const [user, setUser] = useState(null);
========
// import { storage } from "../../utils/firebase"; 

export default function Videos({ user, getAccessTokenSilently, isAuthenticated }) {
>>>>>>>> dailyFocusPopravci:frontend/src/components/home/tabPanel/video/Videos.jsx
    const [videos, setVideos] = useState([]);

    // UPLOAD STATES
    const [showUploadModal, setShowUploadModal] = useState(false);
    const [contentType, setContentType] = useState("VIDEO");
    const [newVideo, setNewVideo] = useState({ title: "", description: "" });
    const [file, setFile] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isUploading, setIsUploading] = useState(false);

    const navigate = useNavigate();
    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";

    // HELPER: FIXED DETECTION LOGIC
    const getMediaType = (item) => {
        if (item.type && (item.type === 'AUDIO' || item.type === 'BLOG')) return item.type;

        if (!item.url) return "VIDEO";
        const urlLower = item.url.toLowerCase();

        // Debugging log
        // console.log(`Checking ${item.title}:`, urlLower);

        if (urlLower.includes('.mp3') || urlLower.includes('.wav') || urlLower.includes('.ogg') || urlLower.includes('audio')) return "AUDIO";
        if (urlLower.includes('.txt') || urlLower.includes('.md') || urlLower.includes('.pdf') || urlLower.includes('document')) return "BLOG";

        return "VIDEO";
    };

    const fetchVideos = useCallback(async () => {
        try {
            const res = await fetch(`${BACKEND_URL}/api/videos`);
            if (res.ok) {
                const data = await res.json();
                setVideos(data);
            }
        } catch (err) {
            console.error("Error fetching content:", err);
        }
    }, [BACKEND_URL]);

    useEffect(() => {
        fetchVideos();
    }, [fetchVideos]);


    const handleFileChange = (e) => {
        if (e.target.files[0]) setFile(e.target.files[0]);
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) return alert("Please select a file first.");

        setIsUploading(true);
        setUploadProgress(10);

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated && getAccessTokenSilently) {
                token = await getAccessTokenSilently({
                    authorizationParams: { 
                        audience: `${BACKEND_URL}`,
                        scope: "openid profile email" },
                });
            }

            const formData = new FormData();
            formData.append("title", newVideo.title);
            formData.append("description", newVideo.description);
            formData.append("type", contentType);
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
                setNewVideo({ title: "", description: "" });
                setFile(null);
                setContentType("VIDEO");
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


    const getAcceptedFileTypes = () => {
        if (contentType === "AUDIO") return "audio/*";
        if (contentType === "BLOG") return ".txt,.md,.pdf";
        return "video/*";
    };

    if (!user) return <div>Loading user data...</div>;

    return (
<<<<<<<< HEAD:frontend/src/components/home/tabPanel/Videos/Videos.jsx
        <>
        <div className={styles.mainContent}>
========
                    <div className={styles.mainContent}>
>>>>>>>> dailyFocusPopravci:frontend/src/components/home/tabPanel/video/Videos.jsx
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h1>Content Library</h1>
                            {user?.role === 'TRAINER' && (
                                <button className={styles.uploadButton} onClick={() => setShowUploadModal(true)}>
                                    Upload New Content
                                </button>
                            )}
                        </div>

                        <div className={styles.videoGrid}>
                            {videos.map((item) => {
                                const itemType = getMediaType(item);

                                return (
                                    <div key={item.id} className={styles.videoCard}>
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
                        </div>
<<<<<<<< HEAD:frontend/src/components/home/tabPanel/Videos/Videos.jsx
        </div>

        {showUploadModal && (
                <div className={styles.modalOverlay}>
========
            
            {showUploadModal && (
>>>>>>>> dailyFocusPopravci:frontend/src/components/home/tabPanel/video/Videos.jsx
                    <div className={styles.modalContent}>
                        <h2>Upload Content</h2>
                        <form onSubmit={handleUpload}>
                            <div className={styles.formGroup}>
                                <label>Content Type</label>
                                <select
                                    value={contentType}
                                    onChange={(e) => setContentType(e.target.value)}
                                    className={styles.selectInput}
                                >
                                    <option value="VIDEO">Video</option>
                                    <option value="AUDIO">Audio (MP3)</option>
                                    <option value="BLOG">Blog (Text File)</option>
                                </select>
                            </div>
                            <div className={styles.formGroup}>
                                <label>Title</label>
                                <input type="text" value={newVideo.title} onChange={(e) => setNewVideo({ ...newVideo, title: e.target.value })} required />
                            </div>
                            <div className={styles.formGroup}>
                                <label>Description</label>
                                <textarea value={newVideo.description} onChange={(e) => setNewVideo({ ...newVideo, description: e.target.value })} required />
                            </div>
                            <div className={styles.formGroup}>
                                <label>File</label>
                                <input type="file" accept={getAcceptedFileTypes()} onChange={handleFileChange} required />
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
            )}
        </>
    );
}