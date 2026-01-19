package Pomna_Sedmica.Mindfulnes.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "practice_plans",
        indexes = {
                @Index(name = "idx_plan_user_validFrom", columnList = "userId,validFrom"),
                @Index(name = "idx_plan_user_validTo", columnList = "userId,validTo")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate validFrom;   // npr. danas

    @Column(nullable = false)
    private LocalDate validTo;     // danas + 6 (ukupno 7 dana)

    @Column(nullable = false)
    private Instant generatedAt;

    // Koji template je korišten (za debug/obranu)
    @Column(nullable = false)
    private String templateKey;

    // Plan kao JSON string (da ne kompliciramo tablice)
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String planJson;
}
