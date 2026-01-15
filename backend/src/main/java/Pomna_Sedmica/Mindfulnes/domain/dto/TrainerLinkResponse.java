package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.UserTrainer;

import java.time.Instant;

public record TrainerLinkResponse(
        Long userId,
        Long trainerId,
        boolean primary,
        Instant createdAt
) {
    public static TrainerLinkResponse from(UserTrainer ut) {
        return new TrainerLinkResponse(
                ut.getUserId(),
                ut.getTrainerId(),
                ut.isPrimaryTrainer(),
                ut.getCreatedAt()
        );
    }
}
