package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.UserTrainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTrainerRepository extends JpaRepository<UserTrainer, Long> {

    List<UserTrainer> findAllByUserId(Long userId);

    List<UserTrainer> findAllByTrainerId(Long trainerId);

    Optional<UserTrainer> findByUserIdAndTrainerId(Long userId, Long trainerId);

    Optional<UserTrainer> findByUserIdAndPrimaryTrainerTrue(Long userId);

    void deleteByUserIdAndTrainerId(Long userId, Long trainerId);
}
