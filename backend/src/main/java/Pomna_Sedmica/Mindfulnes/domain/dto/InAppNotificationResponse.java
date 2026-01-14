package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.InAppNotification;

import java.time.Instant;

public record InAppNotificationResponse(
        Long id,
        String title,
        String message,
        Instant createdAt,
        Instant scheduledStartAt,
        boolean read
) {
    public static InAppNotificationResponse from(InAppNotification n) {
        return new InAppNotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getCreatedAt(),
                n.getScheduledStartAt(),
                n.isRead()
        );
    }
}
