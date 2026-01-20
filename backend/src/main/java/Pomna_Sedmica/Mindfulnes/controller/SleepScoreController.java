package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.service.SleepScoreService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sleep/scores")
@RequiredArgsConstructor
public class SleepScoreController {

    private final UserService userService;
    private final SleepScoreService sleepScoreService;

    /**
     * List scores for the current user.
     *
     * If from/to are omitted, it returns the last 30 days.
     * If provider is omitted, defaults to MOCK (handy for local demo).
     */
    @GetMapping("/me")
    public ResponseEntity<List<SleepScoreResponse>> listMyScores(@AuthenticationPrincipal Jwt jwt,
                                                                 @RequestParam(required = false) SleepProvider provider,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                 LocalDate from,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                 LocalDate to) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        SleepProvider p = (provider == null) ? SleepProvider.MOCK : provider;

        // Existing behaviour: fetch range then filter in-memory.
        // OK to keep, but summary endpoint uses the new provider-scoped query.
        var list = sleepScoreService.listForUser(me.getId(), from, to)
                .stream()
                .filter(s -> s.getProvider() == p)
                .map(SleepScoreResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/me/{date}")
    public ResponseEntity<SleepScoreResponse> getMyScore(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                         LocalDate date,
                                                         @RequestParam(required = false) SleepProvider provider) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        SleepProvider p = (provider == null) ? SleepProvider.MOCK : provider;

        var score = sleepScoreService.getForUserAndDate(me.getId(), date, p)
                .orElseThrow(() -> new IllegalArgumentException("No sleep score for date=" + date + " provider=" + p));

        return ResponseEntity.ok(SleepScoreResponse.from(score));
    }

    // NEW: latest available score (e.g. "zadnja noc")
    @GetMapping("/me/latest")
    public ResponseEntity<SleepScoreResponse> getMyLatestScore(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam(required = false) SleepProvider provider) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        SleepProvider p = (provider == null) ? SleepProvider.MOCK : provider;

        var score = sleepScoreService.getLatestForUser(me.getId(), p)
                .orElseThrow(() -> new IllegalArgumentException("No sleep score found for provider=" + p));

        return ResponseEntity.ok(SleepScoreResponse.from(score));
    }
}
