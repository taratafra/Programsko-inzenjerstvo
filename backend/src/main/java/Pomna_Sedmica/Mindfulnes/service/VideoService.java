package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.CreateVideoRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import Pomna_Sedmica.Mindfulnes.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final com.google.cloud.storage.Bucket storageBucket;

    public VideoService(VideoRepository videoRepository, CommentRepository commentRepository, com.google.cloud.storage.Bucket storageBucket) {
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.storageBucket = storageBucket;
    }

    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VideoResponseDTO createVideo(String title, String description, String type, String goal, String level, Integer duration, org.springframework.web.multipart.MultipartFile file, User trainer) throws java.io.IOException {
        String fileName = "videos/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        com.google.cloud.storage.Blob blob = storageBucket.create(
                fileName,
                file.getInputStream(),
                file.getContentType()
        );

        String url = blob.signUrl(7, java.util.concurrent.TimeUnit.DAYS).toString();

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUrl(url);
        video.setTrainer(trainer);
        try {
            video.setType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.valueOf(type));
        } catch (Exception e) {
            video.setType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.VIDEO);
        }
        
        try {
            video.setGoal(Pomna_Sedmica.Mindfulnes.domain.enums.Goal.valueOf(goal));
        } catch (Exception e) {
            // optional
        }
        try {
            video.setLevel(Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience.valueOf(level));
        } catch (Exception e) {
            // optional
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
        if (video.getTrainer() != null) {
            dto.setTrainerId(video.getTrainer().getId());
            dto.setTrainerName(video.getTrainer().getName() + " " + video.getTrainer().getSurname());
        } else {
            dto.setTrainerId(null);
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

                    // Podcast specific filters (in hours)
                    if ("AUDIO".equals(type)) {
                        switch (durationRange) {
                            case "short": return v.getDuration() < 60;
                            case "long": return v.getDuration() >= 60 && v.getDuration() <= 180;
                            case "superlong": return v.getDuration() > 180;
                            default: return true;
                        }
                    }

                    // Video specific filters (in minutes)
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


    @Transactional
    public void deleteVideoFromDb(Long videoId, User trainer) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        if (video.getTrainer() == null || trainer == null || trainer.getId() == null
                || !video.getTrainer().getId().equals(trainer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own videos");
        }

        commentRepository.deleteByVideoId(videoId);
        videoRepository.delete(video);
    }
}
