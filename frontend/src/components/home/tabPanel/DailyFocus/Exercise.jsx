import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../Videos/Videos.module.css';

export default function SingleVideoPanel({ videoId = null }) {
  const [video, setVideo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";

  const getMediaType = (item) => {
    if (item.type && (item.type === 'AUDIO' || item.type === 'BLOG')) return item.type;
    if (!item.url) return "VIDEO";
    
    const urlLower = item.url.toLowerCase();
    if (urlLower.includes('.mp3') || urlLower.includes('.wav') || urlLower.includes('.ogg') || urlLower.includes('audio')) return "AUDIO";
    if (urlLower.includes('.txt') || urlLower.includes('.md') || urlLower.includes('.pdf') || urlLower.includes('document')) return "BLOG";
    
    return "VIDEO";
  };

  useEffect(() => {
    fetchVideo();
  }, [videoId]);

  const fetchVideo = async () => {
    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${BACKEND_URL}/api/videos`);
      if (!res.ok) throw new Error('Failed to fetch videos');

      const data = await res.json();
      
      if (data.length === 0) {
        setError('No videos available');
        setLoading(false);
        return;
      }

      // Različite strategije odabira videa:
      let selectedVideo;

      if (videoId) {
        // Ako je specificiran ID, traži taj video
        selectedVideo = data.find(v => v.id === videoId);
        if (!selectedVideo) {
          setError('Video not found');
          setLoading(false);
          return;
        }
      } else {
        //ovo se treba dogovoriti sa backendom kako ce oni oznaciti videe koji su za meditaciju/exercise

        // Opcija 1: Prikaži najnoviji video (prvi u listi)
        selectedVideo = data[0];

        // Opcija 2: Random video (zakomentiraj gornju liniju i odkomentiraj ovu)
        // selectedVideo = data[Math.floor(Math.random() * data.length)];

        // Opcija 3: Featured video (ako imaš to polje u backendu)
        // selectedVideo = data.find(v => v.featured) || data[0];
      }

      setVideo(selectedVideo);
    } catch (err) {
      console.error('Error fetching video:', err);
      setError('Failed to load video');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.singleVideoPanel}>
        <div className={styles.loadingContainer}>
          <p>Loading video...</p>
        </div>
      </div>
    );
  }

  if (error || !video) {
    return (
      <div className={styles.singleVideoPanel}>
        <div className={styles.errorContainer}>
          <p>{error || 'No video available'}</p>
        </div>
      </div>
    );
  }

  const itemType = getMediaType(video);

  return (
    <div className={styles.singleVideoPanel}>      
      <div className={styles.featuredVideoCard}>
        {/* Video Thumbnail/Preview */}
        <div
          className={styles.featuredThumbnail}
          onClick={() => navigate(`/watch/${video.id}`, { state: { type: itemType } })}
        >
          {/* Render content based on type */}
          {itemType === 'VIDEO' && (
            <video 
              src={video.url} 
              muted 
              width="100%" 
              height="100%" 
              style={{ objectFit: 'cover', pointerEvents: 'none' }} 
            />
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
          <span className={styles.typeBadge}>{itemType}</span>

          {/* Play Overlay */}
          <div className={styles.playOverlay}>
            <div className={styles.playButton}>▶</div>
          </div>
        </div>

        {/* Video Info */}
        <div className={styles.featuredVideoInfo}>
          <h3 
            className={styles.featuredTitle}
            onClick={() => navigate(`/watch/${video.id}`, { state: { type: itemType } })}
          >
            {video.title}
          </h3>
          <p className={styles.featuredDescription}>{video.description}</p>
          <div className={styles.featuredMeta}>
            <span className={styles.metaType}>{itemType}</span>
            <span className={styles.metaDivider}>•</span>
            <span className={styles.metaTrainer}>By {video.trainerName}</span>
          </div>
        </div>
      </div>
    </div>
  );
}