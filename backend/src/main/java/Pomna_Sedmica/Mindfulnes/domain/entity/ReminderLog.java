package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "reminder_log",
        uniqueConstraints = {
                // Ne želimo duplikate za isti schedule i isti reminder trenutak
                @UniqueConstraint(name = "uk_reminder_schedule_due_channel",
                        columnNames = {"schedule_id", "due_at", "channel"})
        },
        indexes = {
                @Index(name = "idx_reminder_due_at", columnList = "due_at"),
                @Index(name = "idx_reminder_user_id", columnList = "user_id"),
                @Index(name = "idx_reminder_schedule_id", columnList = "schedule_id")
        }
)
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="schedule_id", nullable = false)
    private Long scheduleId;

    // Kada je podsjetnik trebao biti poslan (npr. 10 min prije starta)
    @Column(name="due_at", nullable = false)
    private Instant dueAt;

    // Kada je stvarno poslan
    @Column(name="sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status;

    @Column(name="error_message", length = 1000)
    private String errorMessage;
}
