package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    /**
     * Provider-specific user identifier (e.g., Fitbit user_id).
     */
    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column(name = "access_token", length = 4000)
    private String accessToken;

    @Column(name = "refresh_token", length = 4000)
    private String refreshToken;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "scope", length = 2000)
    private String scope;

    /**
     * Token expiry in UTC (optional but recommended).
     */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) connectedAt = LocalDateTime.now();
    }
}
