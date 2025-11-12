package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.OnboardingSurveyRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.OnboardingSurveyResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository;
import Pomna_Sedmica.Mindfulnes.service.CurrentUserService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingSurveyRepository surveys;
    private final UserService userService;

    public OnboardingController(OnboardingSurveyRepository surveys, UserService userService) {
        this.surveys = surveys;
        this.userService = userService;
    }


    // ---------- Stare rute s {userId} (korisno za dev/M2M test) ----------

    /** Vrati upitnik za danog userId-a (204 ako ne postoji). */
    @GetMapping("/survey/{userId}")
    public ResponseEntity<OnboardingSurveyResponse> getSurvey(@PathVariable Long userId) {
        return surveys.findByUserId(userId)
                .map(OnboardingSurveyResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Kreiraj upitnik za danog userId-a (409 ako već postoji). */
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

    // ---------- NOVE /me rute (za Auth0 user tokene) ----------

    /** Vrati moj upitnik (204 ako ne postoji). Radi samo s USER tokenom (ne M2M). */
    @GetMapping("/survey/me")
    public ResponseEntity<OnboardingSurveyResponse> getMySurvey(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        return surveys.findByUserId(me.getId())
                .map(OnboardingSurveyResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Kreiraj moj upitnik (409 ako vec postoji). Radi samo s USER tokenom (ne M2M). */
    @PostMapping("/survey/me")
    public ResponseEntity<OnboardingSurveyResponse> createMySurvey(@AuthenticationPrincipal Jwt jwt,
                                                                   @Valid @RequestBody OnboardingSurveyRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        if (surveys.existsByUserId(me.getId())) {
            return ResponseEntity.status(409).build();
        }
        var entity = OnboardingSurvey.builder()
                .userId(me.getId())
                .stressLevel(req.stressLevel())
                .sleepQuality(req.sleepQuality())
                .meditationExperience(req.meditationExperience())
                .goals(req.goals())
                .note(req.note())
                .build();
        var saved = surveys.save(entity);
        return ResponseEntity
                .created(URI.create("/onboarding/survey/me"))
                .body(OnboardingSurveyResponse.from(saved));
    }

    /** Azuriraj moj upitnik (404 ako ne postoji). Radi samo s USER tokenom (ne M2M). */
    @PutMapping("/survey/me")
    public ResponseEntity<OnboardingSurveyResponse> updateMySurvey(@AuthenticationPrincipal Jwt jwt,
                                                                   @Valid @RequestBody OnboardingSurveyRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        var survey = surveys.findByUserId(me.getId()).orElse(null);
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
