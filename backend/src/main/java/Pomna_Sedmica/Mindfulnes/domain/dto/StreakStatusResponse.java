package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.UserStreak;

import java.time.LocalDate;

public record StreakStatusResponse(
        Long userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastCompletedDate
) {
    public static StreakStatusResponse from(UserStreak s) {
        return new StreakStatusResponse(
                s.getUserId(),
                s.getCurrentStreak(),
                s.getLongestStreak(),
                s.getLastCompletedDate()
        );
    }
}
