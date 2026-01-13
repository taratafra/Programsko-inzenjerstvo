import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Videos.module.css";
import { storage } from "../../utils/firebase";
import { ref, uploadBytesResumable, getDownloadURL } from "firebase/storage";

import Header from "../../components/home/Header";
import LeftSidebar from "../../components/home/LeftSidebar";
import RightSidebar from "../../components/home/RightSidebar";

export default function Videos() {
    const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
    const [user, setUser] = useState(null);
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showUploadModal, setShowUploadModal] = useState(false);
    const [newVideo, setNewVideo] = useState({ title: "", description: "" });
    const [videoFile, setVideoFile] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isUploading, setIsUploading] = useState(false);
    const navigate = useNavigate();

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";

    const fetchVideos = useCallback(async () => {
        try {
            const res = await fetch(`${BACKEND_URL}/api/videos`);
            if (res.ok) {
                const data = await res.json();
                setVideos(data);
            }
        } catch (err) {
            console.error("Error fetching videos:", err);
        }
    }, [BACKEND_URL]);

    useEffect(() => {
        const init = async () => {
            const localToken = localStorage.getItem("token");

            try {
                if (isAuthenticated && auth0User) {
                    // Fetch user details from backend to get role
                    const token = await getAccessTokenSilently({
                        authorizationParams: {
                            audience: `${BACKEND_URL}`,
                            scope: "openid profile email",
                        },
                    });

                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${token}` },
                    });

                    if (res.ok) {
                        const userData = await res.json();
                        setUser(userData);
                    } else {
                        setUser(auth0User); // Fallback
                    }
                } else if (localToken) {
                    const res = await fetch(`${BACKEND_URL}/api/users/me`, {
                        headers: { Authorization: `Bearer ${localToken}` },
                    });
                    if (res.ok) {
                        const data = await res.json();
                        setUser(data);
                    }
                }
            } catch (err) {
                console.error("Error initializing user:", err);
            } finally {
                setLoading(false);
            }
        };

        if (!isLoading) {
            init();
            fetchVideos();
        }
    }, [isLoading, isAuthenticated, BACKEND_URL, getAccessTokenSilently, auth0User, fetchVideos]);

    const handleFileChange = (e) => {
        if (e.target.files[0]) {
            setVideoFile(e.target.files[0]);
        }
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!videoFile) {
            alert("Please select a video file first.");
            return;
        }

        setIsUploading(true);
        setUploadProgress(10); // Fake progress start

        try {
            let token = localStorage.getItem("token");
            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: {
                        audience: `${BACKEND_URL}`,
                        scope: "openid profile email",
                    },
                });
            }

            const formData = new FormData();
            formData.append("title", newVideo.title);
            formData.append("description", newVideo.description);
            formData.append("file", videoFile);

            // Fake progress interval
            const progressInterval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 90) return prev;
                    return prev + 10;
                });
            }, 500);

            const res = await fetch(`${BACKEND_URL}/api/videos`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    // Content-Type is automatically set by browser for FormData
                },
                body: formData,
            });

            clearInterval(progressInterval);
            setUploadProgress(100);

            if (res.ok) {
                setShowUploadModal(false);
                setNewVideo({ title: "", description: "" });
                setVideoFile(null);
                setUploadProgress(0);
                fetchVideos();
                alert("Video uploaded successfully!");
            } else {
                const errorText = await res.text();
                console.error("Upload failed:", errorText);
                alert("Failed to upload video: " + errorText);
            }
        } catch (err) {
            console.error("Error uploading video:", err);
            alert("Error uploading video");
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

    if (loading) return <div>Loading...</div>;

    return (
        <div className={styles.layoutContainer}>
            <div id="o1"></div>
            <div id="o2"></div>
            <div id="o3"></div>

            <div className={styles.dashboardContentWrapper}>
                <Header navigate={navigate} user={user} />

                <div className={styles.mainGrid}>
                    <LeftSidebar
                        user={user}
                        activeTab="Videos"
                        setActiveTab={() => { }}
                        handleLogout={handleLogout}
                    />

                    <div className={styles.mainContent}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h1>Videos</h1>
                            {console.log("Current User:", user)}
                            {user?.role === 'TRAINER' && (
                                <button
                                    className={styles.uploadButton}
                                    onClick={() => setShowUploadModal(true)}
                                >
                                    Upload Video
                                </button>
                            )}
                        </div>

                        <div className={styles.videoGrid}>
                            {videos.map((video) => (
                                <div key={video.id} className={styles.videoCard}>
                                    <div className={styles.videoThumbnail}>
                                        <video
                                            src={video.url}
                                            controls
                                            width="100%"
                                            height="100%"
                                            style={{ objectFit: 'cover' }}
                                        >
                                            Your browser does not support the video tag.
                                        </video>
                                    </div>
                                    <div className={styles.videoInfo}>
                                        <div className={styles.videoTitle}>{video.title}</div>
                                        <div className={styles.videoDescription}>{video.description}</div>
                                        <div style={{ fontSize: '0.8rem', color: '#888' }}>
                                            By {video.trainerName} on {new Date(video.createdAt).toLocaleDateString()}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <RightSidebar navigate={navigate} />
                </div>
            </div>

            {showUploadModal && (
                <div className={styles.modalOverlay}>
                    <div className={styles.modalContent}>
                        <h2>Upload Video</h2>
                        <form onSubmit={handleUpload}>
                            <div className={styles.formGroup}>
                                <label>Title</label>
                                <input
                                    type="text"
                                    value={newVideo.title}
                                    onChange={(e) => setNewVideo({ ...newVideo, title: e.target.value })}
                                    required
                                    disabled={isUploading}
                                />
                            </div>
                            <div className={styles.formGroup}>
                                <label>Description</label>
                                <textarea
                                    value={newVideo.description}
                                    onChange={(e) => setNewVideo({ ...newVideo, description: e.target.value })}
                                    required
                                    disabled={isUploading}
                                />
                            </div>
                            <div className={styles.formGroup}>
                                <label>Video File</label>
                                <input
                                    type="file"
                                    accept="video/*"
                                    onChange={handleFileChange}
                                    required
                                    disabled={isUploading}
                                />
                            </div>

                            {isUploading && (
                                <div style={{ marginBottom: '15px' }}>
                                    <div style={{ width: '100%', backgroundColor: '#eee', borderRadius: '5px' }}>
                                        <div
                                            style={{
                                                width: `${uploadProgress}%`,
                                                height: '10px',
                                                backgroundColor: '#4caf50',
                                                borderRadius: '5px',
                                                transition: 'width 0.3s ease'
                                            }}
                                        />
                                    </div>
                                    <div style={{ textAlign: 'center', fontSize: '0.8rem', marginTop: '5px' }}>
                                        {Math.round(uploadProgress)}%
                                    </div>
                                </div>
                            )}

                            <div className={styles.modalActions}>
                                <button
                                    type="button"
                                    className={styles.cancelButton}
                                    onClick={() => setShowUploadModal(false)}
                                    disabled={isUploading}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className={styles.submitButton}
                                    disabled={isUploading}
                                >
                                    {isUploading ? 'Uploading...' : 'Upload'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
