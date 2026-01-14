package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.CreateVideoRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.service.CurrentUserService;
import Pomna_Sedmica.Mindfulnes.service.VideoService;
import lombok.RequiredArgsConstructor;
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
    private final CurrentUserService currentUserService;

    public VideoController(VideoService videoService, CurrentUserService currentUserService) {
        this.videoService = videoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<VideoResponseDTO> getAllVideos() {
        return videoService.getAllVideos();
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
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws java.io.IOException {
        
        User user = currentUserService.getOrCreate(jwt);
        if (user.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can upload videos");
        }
        return videoService.createVideo(title, description, type, file, user);
    }
}
