package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.CreateVideoRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.VideoResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final com.google.cloud.storage.Bucket storageBucket;

    public VideoService(VideoRepository videoRepository, com.google.cloud.storage.Bucket storageBucket) {
        this.videoRepository = videoRepository;
        this.storageBucket = storageBucket;
    }

    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VideoResponseDTO createVideo(String title, String description, org.springframework.web.multipart.MultipartFile file, User trainer) throws java.io.IOException {
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
        
        Video savedVideo = videoRepository.save(video);
        return mapToDTO(savedVideo);
    }


    private VideoResponseDTO mapToDTO(Video video) {
        VideoResponseDTO dto = new VideoResponseDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setUrl(video.getUrl());
        dto.setCreatedAt(video.getCreatedAt());
        if (video.getTrainer() != null) {
            dto.setTrainerName(video.getTrainer().getName() + " " + video.getTrainer().getSurname());
        } else {
            dto.setTrainerName("Unknown Trainer");
        }
        return dto;
    }
}
