package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer; // <--- NOVI IMPORT
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.TrainerRepository; // <--- NOVI IMPORT
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
    private final TrainerRepository trainerRepository; // <--- DODANO

    // Ažuriran konstruktor
    public VideoController(VideoService videoService,
                           TrainerService trainerService,
                           TrainerRepository trainerRepository) {
        this.videoService = videoService;
        this.trainerService = trainerService;
        this.trainerRepository = trainerRepository;
    }

    @GetMapping
    public List<VideoResponseDTO> getAllVideos(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String goal,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String durationRange) {
        return videoService.getFilteredVideos(type, goal, level, durationRange);
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

        // 1. Dohvati Usera preko postojećeg servisa (za validaciju JWT-a)
        User user = trainerService.getOrCreateTrainerFromJwt(jwt);

        // 2. Provjera Role
        if (user.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can upload videos");
        }

        // 3. KLJUČNA IZMJENA: Dohvati baš Trainer entitet
        // Iako imamo 'user', moramo biti sigurni da imamo 'Trainer' proxy za spremanje u bazu
        Trainer trainer = trainerRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User is marked as Trainer but not found in Trainer table"));

        // 4. Sada šaljemo 'trainer' objekt, što odgovara novoj metodi u VideoService
        return videoService.createVideo(title, description, type, goal, level, duration, file, trainer);
    }
}