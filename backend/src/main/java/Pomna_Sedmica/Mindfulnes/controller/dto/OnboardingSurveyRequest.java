package Pomna_Sedmica.Mindfulnes.controller.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record OnboardingSurveyRequest(
        @NotNull @Min(1) @Max(5) Integer stressLevel,
        @NotNull @Min(1) @Max(5) Integer sleepQuality,
        @NotNull MeditationExperience meditationExperience,
        @NotNull Set<Goal> goals,
        String note
) {}
