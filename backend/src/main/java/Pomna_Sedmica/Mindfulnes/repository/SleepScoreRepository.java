package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepScore;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepScoreRepository extends JpaRepository<SleepScore, Long> {

    Optional<SleepScore> findByUserIdAndDateAndProvider(Long userId, LocalDate date, SleepProvider provider);

    List<SleepScore> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);

    // NEW: Efficient provider-scoped queries
    Optional<SleepScore> findTopByUserIdAndProviderOrderByDateDesc(Long userId, SleepProvider provider);

    List<SleepScore> findByUserIdAndProviderAndDateBetweenOrderByDateAsc(Long userId,
                                                                         SleepProvider provider,
                                                                         LocalDate from,
                                                                         LocalDate to);
}
