package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Video> {
    List<Video> findAllByOrderByCreatedAtDesc();
    List<Video> findByType(Pomna_Sedmica.Mindfulnes.domain.enums.ContentType type);
    List<Video> findByTrainerIdOrderByCreatedAtDesc(Long trainerId);
    List<Video> findByTrainerId(Long trainerId);
}
