package Pomna_Sedmica.Mindfulnes.domain.dto;

import java.time.LocalDate;
import java.util.List;

public record PracticePlanResponse(
        Long id,
        Long userId,
        LocalDate validFrom,
        LocalDate validTo,
        String templateKey,
        List<DayItem> days
) {
    public record DayItem(
            LocalDate date,
            String title,
            String description,
            Integer estimatedMinutes
    ) {}
}
