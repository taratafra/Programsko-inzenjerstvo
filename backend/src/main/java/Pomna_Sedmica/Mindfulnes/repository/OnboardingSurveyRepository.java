package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingSurveyRepository extends JpaRepository<OnboardingSurvey, Long> {
    Optional<OnboardingSurvey> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
