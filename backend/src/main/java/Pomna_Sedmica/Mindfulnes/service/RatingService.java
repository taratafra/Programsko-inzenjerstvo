package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.RatingResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Rating;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.repository.RatingRepository;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final VideoRepository videoRepository;

    public RatingService(RatingRepository ratingRepository, VideoRepository videoRepository) {
        this.ratingRepository = ratingRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional
    public RatingResponseDTO rateVideo(Long videoId, Integer ratingValue, User user) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        Optional<Rating> existingRating = ratingRepository.findByUserAndVideo(user, video);

        Rating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(ratingValue);
        } else {
            // Create new rating
            rating = new Rating(user, video, ratingValue);
        }

        ratingRepository.save(rating);

        return getRatingStats(videoId, user);
    }

    public RatingResponseDTO getRatingStats(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        Double averageRating = ratingRepository.getAverageRatingByVideoId(videoId);
        Long totalRatings = ratingRepository.getCountByVideoId(videoId);

        Integer userRating = null;
        if (user != null) {
            Optional<Rating> userRatingEntity = ratingRepository.findByUserAndVideo(user, video);
            userRating = userRatingEntity.map(Rating::getRating).orElse(null);
        }

        return new RatingResponseDTO(
                videoId,
                averageRating != null ? averageRating : 0.0,
                totalRatings != null ? totalRatings : 0L,
                userRating
        );
    }

    @Transactional
    public void deleteRating(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        Rating rating = ratingRepository.findByUserAndVideo(user, video)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found"));

        ratingRepository.delete(rating);
    }
}