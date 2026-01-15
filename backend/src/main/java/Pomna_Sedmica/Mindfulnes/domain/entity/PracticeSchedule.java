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
@Table(name = "practice_schedule")
public class PracticeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // vlasnik schedule-a
    @Column(nullable = false)
    private Long userId;

    // obavezno: povezani trener (primarni ili override)
    @Column(nullable = false)
    private Long trainerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType;

    // kod DAILY može biti prazno, kod WEEKLY mora imati dane
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "practice_schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
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
        // enabled default
        // (builder može setati, ali ovo spašava greške)
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (reminderMinutesBefore == null) reminderMinutesBefore = 10;
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Zagreb";
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "schedule_excluded_dates", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "excluded_date")
    @Builder.Default
    private Set<LocalDate> excludedDates = new HashSet<>();
}
