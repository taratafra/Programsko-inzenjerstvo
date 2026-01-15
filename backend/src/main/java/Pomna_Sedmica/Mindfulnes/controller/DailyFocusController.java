package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.CompleteDailyFocusRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.DailyFocusResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.StreakStatusResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.DailyFocusService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/daily-focus")
@RequiredArgsConstructor
public class DailyFocusController {

    private final DailyFocusService service;
    private final UserService userService;

    // ------------------------- /me (Auth0 / JWT) -------------------------

    @GetMapping("/me")
    public ResponseEntity<DailyFocusResponse> getMyDailyFocus(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestParam(required = false)
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                              LocalDate date) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        return ResponseEntity.ok(service.getResponse(me.getId(), date));
    }

    @PostMapping("/me/complete")
    public ResponseEntity<DailyFocusResponse> completeMyDailyFocus(@AuthenticationPrincipal Jwt jwt,
                                                                   @Valid @RequestBody(required = false) CompleteDailyFocusRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        LocalDate date = (req == null) ? null : req.date();
        Map<String, String> answers = (req == null) ? null : req.answers();

        return ResponseEntity.ok(service.complete(me.getId(), date, answers));
    }

    @GetMapping("/me/streak")
    public ResponseEntity<StreakStatusResponse> getMyStreak(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        return ResponseEntity.ok(StreakStatusResponse.from(service.getOrCreateStreak(me.getId())));
    }

    // ------------------------- DEV/TEST rute s userId -------------------------

    @GetMapping("/{userId}")
    public ResponseEntity<DailyFocusResponse> getDailyFocus(@PathVariable Long userId,
                                                            @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                            LocalDate date) {
        return ResponseEntity.ok(service.getResponse(userId, date));
    }

    @PostMapping("/{userId}/complete")
    public ResponseEntity<DailyFocusResponse> completeDailyFocus(@PathVariable Long userId,
                                                                 @Valid @RequestBody(required = false) CompleteDailyFocusRequest req) {
        LocalDate date = (req == null) ? null : req.date();
        Map<String, String> answers = (req == null) ? null : req.answers();

        return ResponseEntity.ok(service.complete(userId, date, answers));
    }

    @GetMapping("/{userId}/streak")
    public ResponseEntity<StreakStatusResponse> getStreak(@PathVariable Long userId) {
        return ResponseEntity.ok(StreakStatusResponse.from(service.getOrCreateStreak(userId)));
    }
}
