package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;

import java.time.LocalDate;
import java.util.List;

public record SleepSummaryResponse(
        SleepProvider provider,
        LocalDate rangeFrom,
        LocalDate rangeTo,
        SleepScoreResponse latest,
        List<SleepScoreResponse> last7Days,
        List<SleepSummaryCard> cards
) { }
