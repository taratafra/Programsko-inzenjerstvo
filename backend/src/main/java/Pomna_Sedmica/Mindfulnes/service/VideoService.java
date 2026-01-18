package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer; // Promijenjeno iz User
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.domain.enums.ContentType;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import Pomna_Sedmica.Mindfulnes.repository.TrainerRepository; // Dodano
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final TrainerRepository trainerRepository; // Dodan repozitorij za provjeru
    private final com.google.cloud.storage.Bucket storageBucket;

    // Ažuriran konstruktor
    public VideoService(VideoRepository videoRepository,
                        TrainerRepository trainerRepository,
                        com.google.cloud.storage.Bucket storageBucket) {
        this.videoRepository = videoRepository;
        this.trainerRepository = trainerRepository;
        this.storageBucket = storageBucket;
    }

    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // IZMJENA: Metoda sada prima Trainer objekt umjesto User
    public VideoResponseDTO createVideo(String title,
                                        String description,
                                        String type,
                                        String goal,
                                        String level,
                                        Integer duration,
                                        org.springframework.web.multipart.MultipartFile file,
                                        Trainer trainer) throws IOException { // <-- Trainer tip

        String fileName = "videos/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        com.google.cloud.storage.Blob blob = storageBucket.create(
                fileName,
                file.getInputStream(),
                file.getContentType()
        );

        // Generiranje potpisanog URL-a (vrijedi 7 dana)
        String url = blob.signUrl(7, TimeUnit.DAYS).toString();

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUrl(url);

        // Ovdje je bila greška: sada spremamo Trainer objekt u Trainer polje
        video.setTrainer(trainer);

        // Sigurnije parsiranje Enuma
        try {
            video.setType(ContentType.valueOf(type));
        } catch (Exception e) {
            video.setType(ContentType.VIDEO); // Default
        }

        try {
            if (goal != null && !goal.isEmpty()) {
                video.setGoal(Goal.valueOf(goal));
            }
        } catch (Exception e) {
            // Ignoriraj ili logiraj grešku, polje ostaje null
        }

        try {
            if (level != null && !level.isEmpty()) {
                video.setLevel(MeditationExperience.valueOf(level));
            }
        } catch (Exception e) {
            // Ignoriraj
        }

        video.setDuration(duration);

        Video savedVideo = videoRepository.save(video);
        return mapToDTO(savedVideo);
    }

    public VideoResponseDTO getVideoById(Long id) {
        return videoRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
    }

    private VideoResponseDTO mapToDTO(Video video) {
        VideoResponseDTO dto = new VideoResponseDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setUrl(video.getUrl());
        dto.setCreatedAt(video.getCreatedAt());
        dto.setType(video.getType() != null ? video.getType().name() : "VIDEO");
        dto.setGoal(video.getGoal() != null ? video.getGoal().name() : null);
        dto.setLevel(video.getLevel() != null ? video.getLevel().name() : null);
        dto.setDuration(video.getDuration());

        // Trainer nasljeđuje Usera, pa ovo i dalje radi normalno
        if (video.getTrainer() != null) {
            dto.setTrainerName(video.getTrainer().getName() + " " + video.getTrainer().getSurname());
        } else {
            dto.setTrainerName("Unknown Trainer");
        }
        return dto;
    }

    public List<VideoResponseDTO> getFilteredVideos(String type, String goal, String level, String durationRange) {
        List<Video> videos = videoRepository.findAllByOrderByCreatedAtDesc();

        return videos.stream()
                .filter(v -> type == null || type.isEmpty() || (v.getType() != null && v.getType().name().equals(type)))
                .filter(v -> goal == null || goal.isEmpty() || (v.getGoal() != null && v.getGoal().name().equals(goal)))
                .filter(v -> level == null || level.isEmpty() || (v.getLevel() != null && v.getLevel().name().equals(level)))
                .filter(v -> {
                    if (durationRange == null || durationRange.isEmpty()) return true;
                    if (v.getDuration() == null) return false;

                    // Logika za Audio/Video filtraciju
                    if ("AUDIO".equals(type)) {
                        switch (durationRange) {
                            case "short": return v.getDuration() < 60;
                            case "long": return v.getDuration() >= 60 && v.getDuration() <= 180;
                            case "superlong": return v.getDuration() > 180;
                            default: return true;
                        }
                    }

                    switch (durationRange) {
                        case "5-10": return v.getDuration() >= 5 && v.getDuration() <= 10;
                        case "10-15": return v.getDuration() >= 10 && v.getDuration() <= 15;
                        case "15-20": return v.getDuration() >= 15 && v.getDuration() <= 20;
                        case "20-plus": return v.getDuration() > 20;
                        default: return true;
                    }
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}