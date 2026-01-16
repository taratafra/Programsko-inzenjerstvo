package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Emotion;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "mood_checkins",
        uniqueConstraints = @UniqueConstraint(name = "uk_mood_user_date", columnNames = {"user_id", "checkin_date"})
)
public class MoodCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vlasnik check-ina
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Za koji dan je check-in
    @Column(name = "checkin_date", nullable = false)
    private LocalDate date;

    // Mood 1-10
    @Column(name = "mood_score", nullable = false)
    private Integer moodScore;

    // Multi-odabir emocija
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mood_checkin_emotions", joinColumns = @JoinColumn(name = "checkin_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion")
    @Builder.Default
    private Set<Emotion> emotions = new HashSet<>();

    // 1-10 slideri
    private Integer sleepQuality;  // 1-10
    private Integer stressLevel;   // 1-10
    private Integer focusLevel;    // 1-10

    // Tekstualna polja (front šalje string)
    private String caffeineIntake;
    private String alcoholIntake;
    private String physicalActivity;

    @Column(length = 2000)
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.date == null) this.date = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
