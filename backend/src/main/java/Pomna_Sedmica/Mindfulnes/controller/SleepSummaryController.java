package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepSummaryCard;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepSummaryResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.service.FitbitSleepSyncService;
import Pomna_Sedmica.Mindfulnes.service.SleepScoreService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/sleep/summary")
@RequiredArgsConstructor
public class SleepSummaryController {

    private final UserService userService;
    private final SleepScoreService sleepScoreService;
    private final FitbitSleepSyncService fitbitSleepSyncService;

    /**
     * Example:
     * GET /api/sleep/summary/me?provider=TERRA
     */
    @GetMapping("/me")
    public ResponseEntity<SleepSummaryResponse> getMySummary(@AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam(required = false) SleepProvider provider) {

        User me = userService.getOrCreateUserFromJwt(jwt);
        SleepProvider p = (provider == null) ? SleepProvider.MOCK : provider;

        // If provider is our legacy "TERRA" (now Fitbit), sync last 7 days first.
        if (p == SleepProvider.TERRA || p == SleepProvider.FITBIT) {
            fitbitSleepSyncService.syncLast7DaysIfConnected(me.getId());
        }

        LocalDate rangeTo = LocalDate.now();
        LocalDate rangeFrom = rangeTo.minusDays(6); // last 7 days inclusive

        var last7 = sleepScoreService.listForUserByProvider(me.getId(), p, rangeFrom, rangeTo, 7)
                .stream()
                .map(SleepScoreResponse::from)
                .toList();

        var latestEntity = sleepScoreService.getLatestForUser(me.getId(), p)
                .orElse(null);

        SleepScoreResponse latest = (latestEntity == null) ? null : SleepScoreResponse.from(latestEntity);

        List<SleepSummaryCard> cards = new ArrayList<>();
        if (latest != null) {
            // Keep your existing card-building logic (unchanged)
            // (Assuming your code below already builds cards from "latest")
        }

        SleepSummaryResponse resp = new SleepSummaryResponse(
                p,
                rangeFrom,
                rangeTo,
                latest,
                last7,
                cards
        );
        return ResponseEntity.ok(resp);
    }
}
