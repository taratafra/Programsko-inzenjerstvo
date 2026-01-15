package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.MoodCheckIn;
import Pomna_Sedmica.Mindfulnes.domain.enums.Emotion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record MoodCheckInResponse(
        Long id,
        Long userId,
        LocalDate date,
        Integer moodScore,
        Set<Emotion> emotions,
        Integer sleepQuality,
        Integer stressLevel,
        Integer focusLevel,
        String caffeineIntake,
        String alcoholIntake,
        String physicalActivity,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MoodCheckInResponse from(MoodCheckIn e) {
        return new MoodCheckInResponse(
                e.getId(),
                e.getUserId(),
                e.getDate(),
                e.getMoodScore(),
                e.getEmotions(),
                e.getSleepQuality(),
                e.getStressLevel(),
                e.getFocusLevel(),
                e.getCaffeineIntake(),
                e.getAlcoholIntake(),
                e.getPhysicalActivity(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
