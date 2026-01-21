package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeScheduleRepository extends JpaRepository<PracticeSchedule, Long> {

    List<PracticeSchedule> findAllByUserIdOrderByStartTimeAsc(Long userId);

    Optional<PracticeSchedule> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    List<PracticeSchedule> findByEnabledTrue();

    List<PracticeSchedule> findByUserIdAndEnabledTrue(Long userId);
    List<PracticeSchedule> findAllByTrainerIdOrderByStartTimeAsc(Long trainerId);

}
