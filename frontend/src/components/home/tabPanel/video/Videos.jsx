import { useAuth0 } from "@auth0/auth0-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Videos.module.css";

export default function Videos(props) {
  const auth0 = useAuth0();
  const navigate = useNavigate();

  const isAuthenticated = props?.isAuthenticated ?? auth0.isAuthenticated;
  const getAccessTokenSilently =
    props?.getAccessTokenSilently ?? auth0.getAccessTokenSilently;
  const auth0User = props?.user ?? auth0.user;
  const isLoadingAuth0 = props?.isLoading ?? auth0.isLoading;

  const [me, setMe] = useState(null);
  const [videos, setVideos] = useState([]);
  const [pageLoading, setPageLoading] = useState(true);

  // Upload UI state
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [contentType, setContentType] = useState("VIDEO");
  const [newContent, setNewContent] = useState({ title: "", description: "" });
  const [file, setFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const BACKEND_URL = useMemo(
    () => process.env.REACT_APP_BACKEND || "http://localhost:8080",
    []
  );
  const AUDIENCE = useMemo(
    () => process.env.REACT_APP_AUTH0_AUDIENCE || BACKEND_URL,
    [BACKEND_URL]
  );

  const inferType = (item) => {
    if (item?.type && ["VIDEO", "AUDIO", "BLOG"].includes(item.type)) return item.type;

    const url = (item?.url || "").toLowerCase();
    if (url.includes(".mp3") || url.includes(".wav") || url.includes(".ogg") || url.includes("audio"))
      return "AUDIO";
    if (url.includes(".txt") || url.includes(".md") || url.includes(".pdf") || url.includes("document"))
      return "BLOG";
    return "VIDEO";
  };

  const getAcceptedFileTypes = () => {
    if (contentType === "AUDIO") return "audio/*";
    if (contentType === "BLOG") return ".txt,.md,.pdf";
    return "video/*";
  };

  const getToken = useCallback(async () => {
    // Local JWT flow
    const localToken = localStorage.getItem("token");
    if (!isAuthenticated) return localToken || null;

    // Auth0 flow
    try {
      return await getAccessTokenSilently({
        authorizationParams: {
          audience: `${AUDIENCE}`,
          scope: "openid profile email",
        },
      });
    } catch (e) {
      console.error("Failed to get Auth0 token:", e);
      return null;
    }
  }, [AUDIENCE, getAccessTokenSilently, isAuthenticated]);

  const fetchMe = useCallback(async () => {
    const token = await getToken();
    if (!token) return null;

    try {
      const res = await fetch(`${BACKEND_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return null;
      return await res.json();
    } catch (e) {
      console.error("Failed to fetch /api/users/me:", e);
      return null;
    }
  }, [BACKEND_URL, getToken]);

  const fetchVideos = useCallback(async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/videos`);
      if (!res.ok) return;
      const data = await res.json();
      setVideos(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error("Failed to fetch /api/videos:", e);
    }
  }, [BACKEND_URL]);

  useEffect(() => {
    const init = async () => {
      setPageLoading(true);

      if (isAuthenticated && isLoadingAuth0) return;

      const hasLocalToken = !!localStorage.getItem("token");
      if (!isAuthenticated && !hasLocalToken) {
        setPageLoading(false);
        navigate("/login");
        return;
      }

      const backendMe = await fetchMe();
      if (backendMe) {
        setMe(backendMe);
        if (backendMe.isOnboardingComplete === false) {
          setPageLoading(false);
          navigate("/questions", { replace: true });
          return;
        }
      } else {
        setMe(auth0User || null);
      }

      await fetchVideos();
      setPageLoading(false);
    };

    init();
  }, [isAuthenticated, isLoadingAuth0, fetchMe, fetchVideos, navigate]);

  const handleFileChange = (e) => {
    const f = e?.target?.files?.[0];
    setFile(f || null);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      alert("Please select a file first.");
      return;
    }

    const token = await getToken();
    if (!token) {
      alert("Not authenticated.");
      return;
    }

    setIsUploading(true);
    setUploadProgress(10);

    const interval = setInterval(() => {
      setUploadProgress((p) => (p >= 90 ? p : p + 10));
    }, 400);

    try {
      const formData = new FormData();
      formData.append("title", newContent.title);
      formData.append("description", newContent.description);
      formData.append("type", contentType);
      formData.append("file", file);

      const res = await fetch(`${BACKEND_URL}/api/videos`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });

      clearInterval(interval);
      setUploadProgress(100);

      if (!res.ok) {
        const text = await res.text().catch(() => "");
        alert(`Upload failed: ${text || res.status}`);
        return;
      }

      setShowUploadModal(false);
      setContentType("VIDEO");
      setNewContent({ title: "", description: "" });
      setFile(null);
      setUploadProgress(0);
      await fetchVideos();
      alert("Upload successful!");
    } catch (err) {
      console.error("Upload error:", err);
      alert("Error uploading file.");
    } finally {
      clearInterval(interval);
      setIsUploading(false);
    }
  };

  if (pageLoading) return <div>Loading...</div>;

  return (
    <>
      <div className={styles.mainContent}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 20,
          }}
        >
          <h1>Content Library</h1>

          {me?.role === "TRAINER" && (
            <button
              className={styles.uploadButton}
              onClick={() => setShowUploadModal(true)}
            >
              Upload New Content
            </button>
          )}
        </div>

        <div className={styles.videoGrid}>
          {videos.map((item) => {
            const itemType = inferType(item);

            return (
              <div key={item.id} className={styles.videoCard}>
                <div
                  className={styles.videoThumbnail}
                  style={{ cursor: "pointer" }}
                  onClick={() =>
                    navigate(`/watch/${item.id}`, { state: { type: itemType } })
                  }
                  onMouseEnter={(e) => {
                    if (itemType !== "VIDEO") return;
                    const v = e.currentTarget.querySelector("video");
                    if (v) v.play().catch(() => {});
                  }}
                  onMouseLeave={(e) => {
                    if (itemType !== "VIDEO") return;
                    const v = e.currentTarget.querySelector("video");
                    if (v) {
                      v.pause();
                      v.currentTime = 0;
                    }
                  }}
                >
                  {itemType === "VIDEO" && (
                    <video
                      src={item.url}
                      muted
                      width="100%"
                      height="100%"
                      style={{ objectFit: "cover", pointerEvents: "none" }}
                    />
                  )}

                  {itemType === "AUDIO" && (
                    <div className={`${styles.placeholderIcon} ${styles.audioIcon}`}>
                      🎧
                    </div>
                  )}

                  {itemType === "BLOG" && (
                    <div className={`${styles.placeholderIcon} ${styles.blogIcon}`}>
                      📄
                    </div>
                  )}

                  <span className={styles.typeBadge}>{itemType}</span>
                </div>

                <div className={styles.videoInfo}>
                  <div
                    className={styles.videoTitle}
                    style={{ cursor: "pointer" }}
                    onClick={() =>
                      navigate(`/watch/${item.id}`, { state: { type: itemType } })
                    }
                  >
                    {item.title}
                  </div>
                  <div className={styles.videoDescription}>{item.description}</div>
                  <div style={{ fontSize: "0.8rem", color: "#888" }}>
                    {itemType} • By {item.trainerName}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {showUploadModal && (
        <div className={styles.modalOverlay}>
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
                <input
                  type="text"
                  value={newContent.title}
                  onChange={(e) =>
                    setNewContent((p) => ({ ...p, title: e.target.value }))
                  }
                  required
                />
              </div>

              <div className={styles.formGroup}>
                <label>Description</label>
                <textarea
                  value={newContent.description}
                  onChange={(e) =>
                    setNewContent((p) => ({ ...p, description: e.target.value }))
                  }
                  required
                />
              </div>

              <div className={styles.formGroup}>
                <label>File</label>
                <input
                  type="file"
                  accept={getAcceptedFileTypes()}
                  onChange={handleFileChange}
                  required
                />
              </div>

              {isUploading && (
                <div className={styles.progressContainer}>
                  <div
                    className={styles.progressFill}
                    style={{ width: `${uploadProgress}%` }}
                  />
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
                  {isUploading ? "Uploading..." : "Upload"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
