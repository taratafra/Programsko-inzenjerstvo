package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.Set;

/**
 * Upitnik koji korisnik ispunjava pri prvom koristenju.
 * -stressLevel i sleepQuality su 1..5
 * -meditationExperience je enum
 * -goals je skup checkbox ciljeva
 * -po korisniku postoji tocno jedan upitnik
 */

@Entity
@Table(
        name = "onboarding_surveys",
        uniqueConstraints = @UniqueConstraint(name = "uq_onboarding_user", columnNames = "user_id")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OnboardingSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Razina stresa 1..5 */
    @Min(1) @Max(5)
    @Column(name = "stress_level", nullable = false)
    private Integer stressLevel;

    /** Kvaliteta sna 1..5 */
    @Min(1) @Max(5)
    @Column(name = "sleep_quality", nullable = false)
    private Integer sleepQuality;

    /** Iskustvo s meditacijom */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "meditation_experience", nullable = false, length = 20)
    private MeditationExperience meditationExperience;

    /** Visestruki ciljevi (checkbox) */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "onboarding_goals", joinColumns = @JoinColumn(name = "survey_id"))
    @Column(name = "goal", nullable = false, length = 40)
    private Set<Goal> goals;

    /** Dodatna biljeska korisnika */
    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "session_length")
    private String sessionLength;

    @Column(name = "preferred_time")
    private String preferredTime;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
