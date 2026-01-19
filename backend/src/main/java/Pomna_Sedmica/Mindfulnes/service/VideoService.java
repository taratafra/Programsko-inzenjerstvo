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

    public VideoResponseDTO createVideo(String title, String description, String type, org.springframework.web.multipart.MultipartFile file, User trainer) throws java.io.IOException {
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
        if (video.getTrainer() != null) {
            dto.setTrainerId(video.getTrainer().getId());
            dto.setTrainerName(video.getTrainer().getName() + " " + video.getTrainer().getSurname());
        } else {
            dto.setTrainerId(null);
            dto.setTrainerName("Unknown Trainer");
        }
        return dto;
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
