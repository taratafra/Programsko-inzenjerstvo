package Pomna_Sedmica.Mindfulnes.controller.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;

import java.util.Set;

/**Izlazni JSON prema frontendu*/
public record OnboardingSurveyResponse(
        Integer stressLevel,
        Integer sleepQuality,
        MeditationExperience meditationExperience,
        Set<Goal> goals,
        String note,
        String updatedAt
) {
    public static OnboardingSurveyResponse from(OnboardingSurvey s) {
        return new OnboardingSurveyResponse(
                s.getStressLevel(),
                s.getSleepQuality(),
                s.getMeditationExperience(),
                s.getGoals(),
                s.getNote(),
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null
        );
    }
}
