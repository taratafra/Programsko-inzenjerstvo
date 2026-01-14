package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "practice_schedules")
public class PracticeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "practice_schedule_days",
            joinColumns = @JoinColumn(name = "schedule_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day")
    @Builder.Default
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    private LocalDate date;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private Integer reminderMinutesBefore;


    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

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
