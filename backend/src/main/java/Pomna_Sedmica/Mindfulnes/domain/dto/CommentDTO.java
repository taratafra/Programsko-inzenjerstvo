package Pomna_Sedmica.Mindfulnes.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDTO {
    private Long id;
    private String text;
    private String authorName;
    private String authorPicture;
    private LocalDateTime createdAt;
    private List<CommentDTO> replies;
    private boolean isOwner;

    public CommentDTO(Long id, String text, String authorName, String authorPicture, LocalDateTime createdAt, List<CommentDTO> replies, boolean isOwner) {
        this.id = id;
        this.text = text;
        this.authorName = authorName;
        this.authorPicture = authorPicture;
        this.createdAt = createdAt;
        this.replies = replies;
        this.isOwner = isOwner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorPicture() {
        return authorPicture;
    }

    public void setAuthorPicture(String authorPicture) {
        this.authorPicture = authorPicture;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CommentDTO> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentDTO> replies) {
        this.replies = replies;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }
}
