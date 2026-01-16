package Pomna_Sedmica.Mindfulnes.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "daily_focus",
        uniqueConstraints = @UniqueConstraint(name = "uk_focus_user_date", columnNames = {"user_id", "focus_date"})
)
public class DailyFocus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="focus_date", nullable=false)
    private LocalDate date;

    // npr. 3 pitanja za raspoloženje / refleksiju
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="daily_focus_questions", joinColumns=@JoinColumn(name="daily_focus_id"))
    @Column(name="question_id", nullable=false)
    @Builder.Default
    private List<String> selectedQuestionIds = new ArrayList<>();

    @Column(nullable=false)
    private Boolean completed;

    private Instant completedAt;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (completed == null) completed = false;
        if (createdAt == null) createdAt = Instant.now();
        if (date == null) date = LocalDate.now();
    }
}
