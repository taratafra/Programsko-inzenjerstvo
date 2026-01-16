package Pomna_Sedmica.Mindfulnes.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_streaks", uniqueConstraints = @UniqueConstraint(name = "uk_streak_user", columnNames = {"user_id"}))
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, unique=true)
    private Long userId;

    @Column(nullable = false)
    private Integer currentStreak;

    @Column(nullable = false)
    private Integer longestStreak;

    private LocalDate lastCompletedDate;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (currentStreak == null) currentStreak = 0;
        if (longestStreak == null) longestStreak = 0;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
