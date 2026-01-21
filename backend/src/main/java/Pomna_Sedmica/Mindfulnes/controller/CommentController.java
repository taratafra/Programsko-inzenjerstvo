package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.CommentDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.CommentService;
import Pomna_Sedmica.Mindfulnes.service.TrainerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;
    private final TrainerService trainerService; // Using TrainerService to get user from JWT

    public CommentController(CommentService commentService, TrainerService trainerService) {
        this.commentService = commentService;
        this.trainerService = trainerService;
    }

    @GetMapping("/videos/{videoId}/comments")
    public List<CommentDTO> getComments(@PathVariable Long videoId, @AuthenticationPrincipal Jwt jwt) {
        User user = null;
        if (jwt != null) {
            try {
                user = trainerService.getOrCreateTrainerFromJwt(jwt);
            } catch (Exception e) {
                // Ignore if user cannot be fetched, treat as guest for viewing
            }
        }
        return commentService.getCommentsForVideo(videoId, user);
    }

    @PostMapping("/videos/{videoId}/comments")
    public CommentDTO addComment(
            @PathVariable Long videoId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        return commentService.addComment(videoId, payload.get("text"), user);
    }

    @PostMapping("/comments/{commentId}/replies")
    public CommentDTO replyToComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        return commentService.replyToComment(commentId, payload.get("text"), user);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        commentService.deleteComment(commentId, user);
    }
}
