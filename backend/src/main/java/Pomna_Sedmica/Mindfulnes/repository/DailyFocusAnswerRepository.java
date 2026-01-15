package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.DailyFocusAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyFocusAnswerRepository extends JpaRepository<DailyFocusAnswer, Long> {

    Optional<DailyFocusAnswer> findByUserIdAndDateAndQuestionId(Long userId, LocalDate date, String questionId);

    List<DailyFocusAnswer> findAllByUserIdAndDate(Long userId, LocalDate date);

    void deleteAllByUserIdAndDate(Long userId, LocalDate date);
}
