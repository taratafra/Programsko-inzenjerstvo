package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserId(Long userId);
}
