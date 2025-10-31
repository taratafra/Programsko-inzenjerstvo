package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyRequest;
import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingSurveyRepository surveys;

    public OnboardingController(OnboardingSurveyRepository surveys) {
        this.surveys = surveys;
    }

    /** Vrati upitnik za danog userId-a (204 ako ne postoji). */
    @GetMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> getSurvey(@PathVariable Long userId) {
        return surveys.findByUserId(userId)
                .map(OnboardingSurveyResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Kreiraj upitnik za danog userId-a (409 ako vec postoji). */
    @PostMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> createSurvey(@PathVariable Long userId,
                                                                 @Valid @RequestBody OnboardingSurveyRequest req) {
        if (surveys.existsByUserId(userId)) {
            return ResponseEntity.status(409).build();
        }
        var entity = OnboardingSurvey.builder()
                .userId(userId)
                .stressLevel(req.stressLevel())
                .sleepQuality(req.sleepQuality())
                .meditationExperience(req.meditationExperience())
                .goals(req.goals())
                .note(req.note())
                .build();

        var saved = surveys.save(entity);
        return ResponseEntity
                .created(URI.create("/onboarding/survey/" + userId))
                .body(OnboardingSurveyResponse.from(saved));
    }

    /** Azuriraj upitnik (404 ako ne postoji). */
    @PutMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> updateSurvey(@PathVariable Long userId,
                                                                 @Valid @RequestBody OnboardingSurveyRequest req) {
        var survey = surveys.findByUserId(userId).orElse(null);
        if (survey == null) return ResponseEntity.notFound().build();

        survey.setStressLevel(req.stressLevel());
        survey.setSleepQuality(req.sleepQuality());
        survey.setMeditationExperience(req.meditationExperience());
        survey.setGoals(req.goals());
        survey.setNote(req.note());
        survey.setUpdatedAt(Instant.now());

        var saved = surveys.save(survey);
        return ResponseEntity.ok(OnboardingSurveyResponse.from(saved));
    }
}
