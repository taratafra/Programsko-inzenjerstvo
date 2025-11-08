package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyRequest;
import Pomna_Sedmica.Mindfulnes.controller.dto.OnboardingSurveyResponse;
import Pomna_Sedmica.Mindfulnes.mapper.OnboardingSurveyMapper; // ← OVDJE!
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
    private final OnboardingSurveyMapper mapper; // ← DODANO

    public OnboardingController(OnboardingSurveyRepository surveys,
                                OnboardingSurveyMapper mapper) { // ← DODANO
        this.surveys = surveys;
        this.mapper = mapper;
    }

    @GetMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> getSurvey(@PathVariable Long userId) {
        return surveys.findByUserId(userId)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> createSurvey(@PathVariable Long userId,
                                                                 @Valid @RequestBody OnboardingSurveyRequest req) {
        if (surveys.existsByUserId(userId)) {
            return ResponseEntity.status(409).build();
        }
        OnboardingSurvey entity = mapper.toEntity(userId, req);
        OnboardingSurvey saved = surveys.save(entity);
        return ResponseEntity
                .created(URI.create("/onboarding/survey/" + userId))
                .body(mapper.toResponse(saved));
    }

    @PutMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> updateSurvey(@PathVariable Long userId,
                                                                 @Valid @RequestBody OnboardingSurveyRequest req) {
        var survey = surveys.findByUserId(userId).orElse(null);
        if (survey == null) return ResponseEntity.notFound().build();

        mapper.updateEntity(survey, req);
        survey.setUpdatedAt(Instant.now());

        var saved = surveys.save(survey);
        return ResponseEntity.ok(mapper.toResponse(saved));
    }
}
