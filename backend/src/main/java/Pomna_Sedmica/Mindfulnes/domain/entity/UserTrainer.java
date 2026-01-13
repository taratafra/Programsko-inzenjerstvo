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
        name = "user_trainer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_trainer_pair", columnNames = {"user_id", "trainer_id"})
        },
        indexes = {
                @Index(name = "idx_user_trainer_user_id", columnList = "user_id"),
                @Index(name = "idx_user_trainer_trainer_id", columnList = "trainer_id")
        }
)
public class UserTrainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User koji je odabrao trenera
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Trainer (User s role TRAINER)
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    // Je li ovo primarni trener
    @Column(name = "is_primary", nullable = false)
    private boolean primaryTrainer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
