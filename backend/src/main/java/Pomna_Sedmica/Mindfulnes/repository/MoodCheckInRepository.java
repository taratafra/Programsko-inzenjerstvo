package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.MoodCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodCheckInRepository extends JpaRepository<MoodCheckIn, Long> {

    Optional<MoodCheckIn> findByUserIdAndDate(Long userId, LocalDate date);

    List<MoodCheckIn> findAllByUserIdOrderByDateDesc(Long userId);

    List<MoodCheckIn> findAllByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);
}
