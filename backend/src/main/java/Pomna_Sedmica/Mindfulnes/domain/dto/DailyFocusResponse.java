package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.DailyFocus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyFocusResponse(
        Long id,
        Long userId,
        LocalDate date,
        List<QuestionItem> questions,
        boolean completed,
        Instant completedAt
) {
    public record QuestionItem(String id, String text) {}

    public static DailyFocusResponse from(DailyFocus f, List<QuestionItem> items) {
        return new DailyFocusResponse(
                f.getId(),
                f.getUserId(),
                f.getDate(),
                items,
                Boolean.TRUE.equals(f.getCompleted()),
                f.getCompletedAt()
        );
    }
}
