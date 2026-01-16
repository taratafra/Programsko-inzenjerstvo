package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.BadgeType;
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
        name = "badge_awards",
        uniqueConstraints = @UniqueConstraint(name = "uk_badge_user_type", columnNames = {"user_id", "badge_type"})
)
public class BadgeAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name="badge_type", nullable=false)
    private BadgeType badgeType;

    @Column(nullable=false)
    private Instant awardedAt;

    @PrePersist
    public void prePersist() {
        if (awardedAt == null) awardedAt = Instant.now();
    }
}
