package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PracticePlanRepository extends JpaRepository<PracticePlan, Long> {

    Optional<PracticePlan> findFirstByUserIdAndValidFromLessThanEqualAndValidToGreaterThanEqualOrderByValidFromDesc(
            Long userId, LocalDate from, LocalDate to
    );

    Optional<PracticePlan> findFirstByUserIdOrderByValidFromDesc(Long userId);
}
