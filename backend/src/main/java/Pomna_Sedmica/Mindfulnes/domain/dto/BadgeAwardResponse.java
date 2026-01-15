package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.BadgeAward;
import Pomna_Sedmica.Mindfulnes.domain.enums.BadgeType;

import java.time.Instant;

public record BadgeAwardResponse(
        Long id,
        Long userId,
        BadgeType badgeType,
        Instant awardedAt
) {
    public static BadgeAwardResponse from(BadgeAward b) {
        return new BadgeAwardResponse(b.getId(), b.getUserId(), b.getBadgeType(), b.getAwardedAt());
    }
}
