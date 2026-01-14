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
  
  const { user: auth0User, isAuthenticated } = useAuth0();
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

  useEffect(() => {
    if (isAuthenticated && auth0User) {
        setUser(auth0User); 
    }
  }, [isAuthenticated, auth0User]);

  useEffect(() => {
    if (contentData.type === 'BLOG') {
        setContentData(prev => ({
            ...prev,
            title: "Healthy Eating Habits (Blog)",
            textBody: "This is a placeholder for the blog content. \n\n1. Eat more greens.\n2. Drink water.\n3. Sleep well.\n\n(This text represents the uploaded .txt file content)"
        }));
    } else if (contentData.type === 'AUDIO') {
        setContentData(prev => ({
            ...prev,
            title: "Morning Meditation (Audio)",
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" 
        }));
    } else {
        setContentData(prev => ({
            ...prev,
            title: "Full Body Workout (Video)",
            url: "https://www.w3schools.com/html/mov_bbb.mp4"
        }));
    }
  }, [id, contentData.type]);

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
                            <p className={styles.videoDate}>Posted on Oct 24, 2025</p>
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
                        <p>This content helps you improve your lifestyle. (Placeholder description)</p>
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