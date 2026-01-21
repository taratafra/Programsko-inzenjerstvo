package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.CommentDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Comment;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.repository.CommentRepository;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    public CommentService(CommentRepository commentRepository, VideoRepository videoRepository) {
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
    }

    public CommentDTO addComment(Long videoId, String text, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        Comment comment = new Comment();
        comment.setText(text);
        comment.setUser(user);
        comment.setVideo(video);

        Comment savedComment = commentRepository.save(comment);
        return mapToDTO(savedComment, user);
    }

    public CommentDTO replyToComment(Long parentId, String text, User user) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));

        Comment reply = new Comment();
        reply.setText(text);
        reply.setUser(user);
        reply.setVideo(parent.getVideo());
        reply.setParentComment(parent);

        Comment savedReply = commentRepository.save(reply);
        return mapToDTO(savedReply, user);
    }

    public List<CommentDTO> getCommentsForVideo(Long videoId, User currentUser) {
        List<Comment> comments = commentRepository.findByVideoIdAndParentCommentIsNullOrderByCreatedAtDesc(videoId);
        return comments.stream()
                .map(comment -> mapToDTO(comment, currentUser))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private CommentDTO mapToDTO(Comment comment, User currentUser) {
        List<CommentDTO> replies = comment.getReplies().stream()
                .map(reply -> mapToDTO(reply, currentUser))
                .collect(Collectors.toList());

        boolean isOwner = currentUser != null && comment.getUser().getId().equals(currentUser.getId());

        return new CommentDTO(
                comment.getId(),
                comment.getText(),
                comment.getUser().getName(),
                comment.getUser().getProfilePictureUrl(),
                comment.getCreatedAt(),
                replies,
                isOwner
        );
    }
}
