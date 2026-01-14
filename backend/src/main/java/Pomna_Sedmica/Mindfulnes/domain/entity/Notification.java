package Pomna_Sedmica.Mindfulnes.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id"),
                @Index(name = "idx_notification_read", columnList = "user_id,is_read"),
                @Index(name = "idx_notification_fire", columnList = "fire_at")
        },
        uniqueConstraints = {
                // sprječava duplikate za isti schedule + vrijeme paljenja
                @UniqueConstraint(name = "uk_notification_once", columnNames = {"user_id", "schedule_id", "fire_at"})
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name="fire_at", nullable = false)
    private Instant fireAt;

    @Column(name="message", nullable = false, length = 500)
    private String message;

    @Column(name="is_read", nullable = false)
    private boolean read;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        // default: unread
    }
}
