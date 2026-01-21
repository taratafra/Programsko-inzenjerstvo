package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.service.TrainerService;
import Pomna_Sedmica.Mindfulnes.service.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final TrainerService trainerService;

    public VideoController(VideoService videoService, TrainerService trainerService) {
        this.videoService = videoService;
        this.trainerService = trainerService;
    }

    @GetMapping
    public org.springframework.data.domain.Page<VideoResponseDTO> getAllVideos(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String goal,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String durationRange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return videoService.getFilteredVideos(type, goal, level, durationRange, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @GetMapping("/recommendations")
    public List<VideoResponseDTO> getRecommendations(@AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        return videoService.getRecommendations(user);
    }

    // NEW: Get all videos uploaded by the authenticated trainer
    @GetMapping("/trainer/me")
    @PreAuthorize("hasRole('TRAINER')")
    public List<VideoResponseDTO> getMyVideos(@AuthenticationPrincipal Jwt jwt) {
        User trainer = trainerService.getOrCreateTrainerFromJwt(jwt);
        if (trainer.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can access this endpoint");
        }
        return videoService.getVideosByTrainer(trainer);
    }

    @GetMapping("/{id}")
    public VideoResponseDTO getVideoById(@PathVariable Long id) {
        return videoService.getVideoById(id);
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TRAINER')")
    public VideoResponseDTO createVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam(value = "goal", required = false) String goal,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws java.io.IOException {

        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        if (user.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can upload videos");
        }
        return videoService.createVideo(title, description, type, goal, level, duration, file, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVideo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);
        if (user.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can delete videos");
        }
        videoService.deleteVideoFromDb(id, user);
    }
}