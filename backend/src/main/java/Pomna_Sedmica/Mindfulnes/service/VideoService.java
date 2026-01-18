package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.domain.enums.ContentType;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import Pomna_Sedmica.Mindfulnes.repository.TrainerRepository;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import com.google.cloud.storage.Bucket; // Importaj ovo!
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final TrainerRepository trainerRepository;
    private final Bucket storageBucket; // Vraćeno polje

    // Vraćen Bucket u konstruktor
    public VideoService(VideoRepository videoRepository,
                        TrainerRepository trainerRepository,
                        Bucket storageBucket) {
        this.videoRepository = videoRepository;
        this.trainerRepository = trainerRepository;
        this.storageBucket = storageBucket;
    }

    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VideoResponseDTO createVideo(String title,
                                        String description,
                                        String type,
                                        String goal,
                                        String level,
                                        Integer duration,
                                        MultipartFile file,
                                        Trainer trainer) throws IOException {

        // --- PRAVI UPLOAD ---
        String fileName = "videos/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        com.google.cloud.storage.Blob blob = storageBucket.create(
                fileName,
                file.getInputStream(),
                file.getContentType()
        );

        // Generiraj potpisani URL (vrijedi 7 dana) ili koristi public URL logiku
        String url = blob.signUrl(7, TimeUnit.DAYS).toString();

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUrl(url);
        video.setTrainer(trainer);

        try {
            video.setType(ContentType.valueOf(type));
        } catch (Exception e) {
            video.setType(ContentType.VIDEO);
        }
        // ... (ostatak settera za goal, level itd. ostaje isti kao prije) ...
        try { if (goal != null && !goal.isEmpty()) video.setGoal(Goal.valueOf(goal)); } catch (Exception e) {}
        try { if (level != null && !level.isEmpty()) video.setLevel(MeditationExperience.valueOf(level)); } catch (Exception e) {}
        video.setDuration(duration);

        Video savedVideo = videoRepository.save(video);
        return mapToDTO(savedVideo);
    }

    // ... metode getVideoById, mapToDTO i getFilteredVideos ostaju iste (s onim fixom za equalsIgnoreCase) ...
    // Ako ih nemaš pri ruci, mogu ti ih ponoviti, ali bitno je samo da vratiš createVideo na staro.

    // Ovdje SAMO kopiraj ostatak klase koji si imao maloprije (getFilteredVideos s equalsIgnoreCase itd.)
    // ...
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
                .filter(v -> type == null || type.isEmpty() || (v.getType() != null && v.getType().name().equalsIgnoreCase(type)))
                .filter(v -> goal == null || goal.isEmpty() || (v.getGoal() != null && v.getGoal().name().equalsIgnoreCase(goal)))
                .filter(v -> level == null || level.isEmpty() || (v.getLevel() != null && v.getLevel().name().equalsIgnoreCase(level)))
                .filter(v -> {
                    if (durationRange == null || durationRange.isEmpty()) return true;
                    if (v.getDuration() == null) return false;

                    if ("AUDIO".equalsIgnoreCase(type)) {
                        switch (durationRange.toLowerCase()) {
                            case "short": return v.getDuration() < 60;
                            case "long": return v.getDuration() >= 60 && v.getDuration() <= 180;
                            case "superlong": return v.getDuration() > 180;
                            default: return true;
                        }
                    }

                    switch (durationRange.toLowerCase()) {
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