package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**Izlazni JSON prema frontendu*/
public record OnboardingSurveyResponse(
        Integer stressLevel,
        Integer sleepQuality,
        MeditationExperience meditationExperience,
        Set<Goal> goals,
        String sessionLength,
        String preferredTime,
        String note,
        String updatedAt
) {
    public static OnboardingSurveyResponse from(OnboardingSurvey s) {
        return new OnboardingSurveyResponse(
                s.getStressLevel(),
                s.getSleepQuality(),
                s.getMeditationExperience(),
                s.getGoals(),
                s.getSessionLength(),
                s.getPreferredTime(),
                s.getNote(),
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null
        );
    }
}
