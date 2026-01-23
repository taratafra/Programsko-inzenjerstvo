package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.RatingResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.Rating;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.RatingRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatingServiceTest {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User trainer;
    private Video video;

    @BeforeEach
    void setup() {
        ratingRepository.deleteAll();
        videoRepository.deleteAll();
        userRepository.deleteAll();

        trainer = new User();
        trainer.setRole(Role.TRAINER);
        trainer = userRepository.save(trainer);

        user = new User();
        user.setRole(Role.USER);
        user = userRepository.save(user);

        video = new Video();
        video.setTrainer(trainer);
        video = videoRepository.save(video);
    }

    @Test
    void shouldCreateNewRating() {
        RatingResponseDTO response =
                ratingService.rateVideo(video.getId(), 5, user);

        assertEquals(5.0, response.averageRating());
        assertEquals(1L, response.totalRatings());
        assertEquals(5, response.userRating());
    }

    @Test
    void shouldUpdateExistingRating() {
        ratingService.rateVideo(video.getId(), 3, user);
        RatingResponseDTO response =
                ratingService.rateVideo(video.getId(), 5, user);

        assertEquals(5.0, response.averageRating());
        assertEquals(1L, response.totalRatings());
        assertEquals(5, response.userRating());
    }

    @Test
    void shouldThrowIfRatingOutOfRange() {
        assertThrows(ResponseStatusException.class,
                () -> ratingService.rateVideo(video.getId(), 6, user));
    }

    @Test
    void shouldThrowIfVideoNotFound() {
        assertThrows(ResponseStatusException.class,
                () -> ratingService.rateVideo(999L, 5, user));
    }

    @Test
    void shouldReturnCorrectStats() {
        ratingService.rateVideo(video.getId(), 4, user);

        RatingResponseDTO stats =
                ratingService.getRatingStats(video.getId(), user);

        assertEquals(4.0, stats.averageRating());
        assertEquals(1L, stats.totalRatings());
        assertEquals(4, stats.userRating());
    }

    @Test
    void shouldReturnZeroIfTrainerHasNoVideos() {
        User newTrainer = new User();
        newTrainer.setRole(Role.TRAINER);
        newTrainer = userRepository.save(newTrainer);

        Map<String, Object> result =
                ratingService.getTrainerAverageRating(newTrainer.getId());

        assertEquals(0.0, result.get("averageRating"));
        assertEquals(0, result.get("totalRatings"));
        assertEquals(0, result.get("totalVideos"));
    }

    @Test
    void shouldReturnZeroIfVideosButNoRatings() {
        Map<String, Object> result =
                ratingService.getTrainerAverageRating(trainer.getId());

        assertEquals(0.0, result.get("averageRating"));
        assertEquals(0, result.get("totalRatings"));
        assertEquals(1, result.get("totalVideos"));
    }

    @Test
    void shouldCalculateTrainerAverageCorrectly() {
        ratingService.rateVideo(video.getId(), 4, user);

        User secondUser = new User();
        secondUser.setRole(Role.USER);
        secondUser = userRepository.save(secondUser);

        ratingService.rateVideo(video.getId(), 5, secondUser);

        Map<String, Object> result =
                ratingService.getTrainerAverageRating(trainer.getId());

        assertEquals(4.5, result.get("averageRating"));
        assertEquals(2, result.get("totalRatings"));
        assertEquals(1, result.get("totalVideos"));
    }

    @Test
    void shouldDeleteRating() {
        ratingService.rateVideo(video.getId(), 4, user);

        ratingService.deleteRating(video.getId(), user);

        assertTrue(ratingRepository.findAll().isEmpty());
    }

    @Test
    void shouldThrowIfDeleteRatingNotFound() {
        assertThrows(ResponseStatusException.class,
                () -> ratingService.deleteRating(video.getId(), user));
    }
}
