package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long scheduleId,
        Instant fireAt,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getScheduleId(),
                n.getFireAt(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
