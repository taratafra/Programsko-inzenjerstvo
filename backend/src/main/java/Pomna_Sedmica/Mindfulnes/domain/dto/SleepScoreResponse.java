package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepScore;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;

import java.time.LocalDate;

public record SleepScoreResponse(
        LocalDate date,
        SleepProvider provider,
        Integer score, // 0-100
        Integer latencyMinutes,
        Integer awakeningsCount,
        Integer continuityScore
) {
    public static SleepScoreResponse from(SleepScore e) {
        return new SleepScoreResponse(
                e.getDate(),
                e.getProvider(),
                e.getScore(),
                e.getLatencyMinutes(),
                e.getAwakeningsCount(),
                e.getContinuityScore()
        );
    }
}
