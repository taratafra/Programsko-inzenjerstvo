package Pomna_Sedmica.Mindfulnes.mapper;

import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyRequest;
import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import org.springframework.stereotype.Component;

@Component
public class OnboardingSurveyMapper {

    /** Kreiraj novi entitet iz DTO-a + userId iz rute. */
    public OnboardingSurvey toEntity(Long userId, OnboardingSurveyRequest req) {
        if (req == null) return null;
        return OnboardingSurvey.builder()
                .userId(userId)
                .stressLevel(req.stressLevel())
                .sleepQuality(req.sleepQuality())
                .meditationExperience(req.meditationExperience())
                .goals(req.goals())
                .note(req.note())
                .build();
    }

    /** Ažuriraj postojeći entitet iz DTO-a. */
    public void updateEntity(OnboardingSurvey entity, OnboardingSurveyRequest req) {
        if (entity == null || req == null) return;
        entity.setStressLevel(req.stressLevel());
        entity.setSleepQuality(req.sleepQuality());
        entity.setMeditationExperience(req.meditationExperience());
        entity.setGoals(req.goals());
        entity.setNote(req.note());
    }

    /** Pretvori entitet u response DTO. */
    public OnboardingSurveyResponse toResponse(OnboardingSurvey s) {
        if (s == null) return null;
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
