package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.RatingRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.RatingResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.RatingService;
import Pomna_Sedmica.Mindfulnes.service.TrainerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
public class RatingController {

    private final RatingService ratingService;
    private final TrainerService trainerService;

    public RatingController(RatingService ratingService, TrainerService trainerService) {
        this.ratingService = ratingService;
        this.trainerService = trainerService;
    }

    @PostMapping("/{videoId}/ratings")
    public RatingResponseDTO rateVideo(
            @PathVariable Long videoId,
            @RequestBody RatingRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        return ratingService.rateVideo(videoId, request.getRating(), user);
    }

    @GetMapping("/{videoId}/ratings")
    public RatingResponseDTO getRatings(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Jwt jwt) {
        User user = jwt != null ? trainerService.getOrCreateTrainerFromJwt(jwt) : null;
        return ratingService.getRatingStats(videoId, user);
    }

    @DeleteMapping("/{videoId}/ratings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        ratingService.deleteRating(videoId, user);
    }
}