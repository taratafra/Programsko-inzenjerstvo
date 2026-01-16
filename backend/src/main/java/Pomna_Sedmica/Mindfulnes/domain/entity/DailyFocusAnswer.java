package Pomna_Sedmica.Mindfulnes.domain.entity;

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
        name = "daily_focus_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_focus_answer",
                columnNames = {"user_id", "focus_date", "question_id"}
        )
)
public class DailyFocusAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="focus_date", nullable=false)
    private java.time.LocalDate date;

    @Column(name="question_id", nullable=false)
    private String questionId;

    @Column(name="answer_text", nullable=false, length = 2000)
    private String answerText;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
