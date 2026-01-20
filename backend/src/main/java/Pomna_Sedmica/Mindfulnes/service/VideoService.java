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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final com.google.cloud.storage.Bucket storageBucket;
    private final Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository onboardingSurveyRepository;

    public VideoService(VideoRepository videoRepository,CommentRepository commentRepository, com.google.cloud.storage.Bucket storageBucket, Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository onboardingSurveyRepository) {
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.storageBucket = storageBucket;
        this.onboardingSurveyRepository = onboardingSurveyRepository;
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


        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUrl(fileName);
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
        dto.setUrl(getSignedUrl(video.getUrl()));
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

    public org.springframework.data.domain.Page<VideoResponseDTO> getFilteredVideos(String type, String goal, String level, String durationRange, Pageable pageable) {
        Specification<Video> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.valueOf(type)));
            }
            if (goal != null && !goal.isEmpty()) {
                predicates.add(cb.equal(root.get("goal"), Pomna_Sedmica.Mindfulnes.domain.enums.Goal.valueOf(goal)));
            }
            if (level != null && !level.isEmpty()) {
                predicates.add(cb.equal(root.get("level"), Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience.valueOf(level)));
            }

            if (durationRange != null && !durationRange.isEmpty()) {
                if ("AUDIO".equals(type)) {
                    switch (durationRange) {
                        case "short":
                            predicates.add(cb.lessThan(root.get("duration"), 60));
                            break;
                        case "long":
                            predicates.add(cb.between(root.get("duration"), 60, 180));
                            break;
                        case "superlong":
                            predicates.add(cb.greaterThan(root.get("duration"), 180));
                            break;
                    }
                } else {
                    switch (durationRange) {
                        case "5-10":
                            predicates.add(cb.between(root.get("duration"), 5, 10));
                            break;
                        case "10-15":
                            predicates.add(cb.between(root.get("duration"), 10, 15));
                            break;
                        case "15-20":
                            predicates.add(cb.between(root.get("duration"), 15, 20));
                            break;
                        case "20-plus":
                            predicates.add(cb.greaterThan(root.get("duration"), 20));
                            break;
                    }
                }
            }
            
            // Order by createdAt desc
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return videoRepository.findAll(spec, pageable).map(this::mapToDTO);
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
    public List<VideoResponseDTO> getRecommendations(User user) {
        List<Video> videos = videoRepository.findByType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.VIDEO);
        List<Video> blogs = videoRepository.findByType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.BLOG);
        List<Video> audios = videoRepository.findByType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType.AUDIO);

        // Fetch user preferences
        java.util.Optional<Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey> surveyOpt = onboardingSurveyRepository.findByUserId(user.getId());

        List<VideoResponseDTO> recommendations = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        if (surveyOpt.isPresent()) {
            Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey survey = surveyOpt.get();
            java.util.Set<Pomna_Sedmica.Mindfulnes.domain.enums.Goal> userGoals = survey.getGoals();
            Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience userLevel = survey.getMeditationExperience();

            // Filter content based on user preferences
            videos = filterContent(videos, userGoals, userLevel);
            blogs = filterContent(blogs, userGoals, userLevel);
            audios = filterContent(audios, userGoals, userLevel);
        }

        if (!videos.isEmpty()) {
            recommendations.add(mapToDTO(videos.get(random.nextInt(videos.size()))));
        }
        if (!blogs.isEmpty()) {
            recommendations.add(mapToDTO(blogs.get(random.nextInt(blogs.size()))));
        }
        if (!audios.isEmpty()) {
            recommendations.add(mapToDTO(audios.get(random.nextInt(audios.size()))));
        }

        return recommendations;
    }

    private List<Video> filterContent(List<Video> content, java.util.Set<Pomna_Sedmica.Mindfulnes.domain.enums.Goal> goals, Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience level) {
        List<Video> filtered = content.stream()
            .filter(v -> v.getLevel() == level || v.getLevel() == null)
            .filter(v -> v.getGoal() == null || goals.contains(v.getGoal()))
            .collect(Collectors.toList());

        return filtered.isEmpty() ? content : filtered;
    }

    private String getSignedUrl(String storedUrl) {
        if (storedUrl == null) return null;

        try {
            com.google.cloud.storage.Blob blob = storageBucket.get(storedUrl);
            if (blob != null) {
                return blob.signUrl(7, java.util.concurrent.TimeUnit.DAYS).toString();
            }
        } catch (Exception e) {
            System.err.println("Error signing URL for blob: " + storedUrl + " - " + e.getMessage());
        }

        return storedUrl;
    }
}
