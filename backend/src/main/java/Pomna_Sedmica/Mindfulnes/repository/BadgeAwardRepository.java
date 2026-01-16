package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.BadgeAward;
import Pomna_Sedmica.Mindfulnes.domain.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeAwardRepository extends JpaRepository<BadgeAward, Long> {
    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
    List<BadgeAward> findAllByUserIdOrderByAwardedAtDesc(Long userId);
}
