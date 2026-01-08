package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "practice_schedules",
        indexes = {
                @Index(name = "idx_practice_schedules_user_id", columnList = "user_id"),
                @Index(name = "idx_practice_schedules_trainer_id", columnList = "trainer_id")
        }
)
public class PracticeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // schedule pripada useru
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // OBAVEZNO: schedule mora imati trenera
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(nullable = false)
    private String title;

    // "08:00"
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false)
    private RepeatType repeatType;

    /**
     * Koristi se samo kad je repeatType = WEEKLY.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "practice_schedule_days",
            joinColumns = @JoinColumn(name = "schedule_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(nullable = false)
    private String timezone;

    @Column(name = "reminder_minutes_before", nullable = false)
    private Integer reminderMinutesBefore;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;

        if (reminderMinutesBefore == null) reminderMinutesBefore = 10;
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Zagreb";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (reminderMinutesBefore == null) reminderMinutesBefore = 10;
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Zagreb";
    }
}
