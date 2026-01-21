package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Normalized sleep score (0..100) for a given date.
 *
 * Some providers provide extra breakdown values (latency / awakenings / continuity). We store them
 * as nullable fields so the UI can explain whichever factors are available.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "sleep_scores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sleep_score_user_date_provider", columnNames = {"user_id", "date", "provider"})
        }
)
public class SleepScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SleepProvider provider;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** 0..100 */
    @Column(name = "score", nullable = false)
    private Integer score;

    /** Minutes until asleep (sleep latency). */
    @Column(name = "latency_minutes")
    private Integer latencyMinutes;

    /** Number of awakenings during the night. */
    @Column(name = "awakenings_count")
    private Integer awakeningsCount;

    /**
     * Continuity is vendor-dependent. Store as 0..100 if you have it, or keep null.
     * (Some providers use 1..5 or 0..1; map it in service.)
     */
    @Column(name = "continuity_score")
    private Integer continuityScore;

    /** Raw provider JSON for debugging (optional). */
    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
