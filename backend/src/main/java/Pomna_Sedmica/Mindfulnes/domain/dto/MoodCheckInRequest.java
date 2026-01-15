package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.Emotion;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

public record MoodCheckInRequest(

        // Ako null => today
        LocalDate date,

        @NotNull
        @Min(1) @Max(10)
        Integer moodScore,

        // Optional: može biti prazno
        Set<Emotion> emotions,

        @Min(1) @Max(10)
        Integer sleepQuality,

        @Min(1) @Max(10)
        Integer stressLevel,

        @Min(1) @Max(10)
        Integer focusLevel,

        String caffeineIntake,
        String alcoholIntake,
        String physicalActivity,

        @Size(max = 2000)
        String notes
) {}
