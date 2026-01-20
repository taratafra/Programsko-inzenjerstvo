package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a link between a Mindfulness user and an external sleep provider.
 *
 * For a real provider, you would usually store OAuth tokens (access/refresh), token expiry,
 * and the provider-specific user id.
 *
 * For the faculty project/demo, storing externalUserId + an optional accessToken is enough.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "sleep_integrations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sleep_integration_user_provider", columnNames = {"user_id", "provider"})
        }
)
public class SleepIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SleepProvider provider;

    /** Provider-specific user identifier (e.g. Terra user_id). */
    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    /** Optional token if you do direct API calls per user. */
    @Column(name = "access_token", length = 2000)
    private String accessToken;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) connectedAt = LocalDateTime.now();
    }
}
