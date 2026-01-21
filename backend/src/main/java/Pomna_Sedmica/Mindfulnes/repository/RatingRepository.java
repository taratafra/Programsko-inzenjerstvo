package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.Rating;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserAndVideo(User user, Video video);

    List<Rating> findByVideo(Video video);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.video.id = :videoId")
    Double getAverageRatingByVideoId(@Param("videoId") Long videoId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.video.id = :videoId")
    Long getCountByVideoId(@Param("videoId") Long videoId);
}