package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepSummaryCard;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepSummaryResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
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

    /**
     * Front-friendly endpoint:
     * - latest ("zadnja noc")
     * - last 7 days (range) for charts/trend
     * - ready-to-render cards with hints (no extra frontend logic needed)
     *
     * Example:
     * GET /api/sleep/summary/me?provider=TERRA
     */
    @GetMapping("/me")
    public ResponseEntity<SleepSummaryResponse> getMySummary(@AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam(required = false) SleepProvider provider) {

        User me = userService.getOrCreateUserFromJwt(jwt);
        SleepProvider p = (provider == null) ? SleepProvider.MOCK : provider;

        LocalDate rangeTo = LocalDate.now();
        LocalDate rangeFrom = rangeTo.minusDays(6); // last 7 days inclusive

        // Provider-scoped list, already ordered asc by date
        var last7 = sleepScoreService.listForUserByProvider(me.getId(), p, rangeFrom, rangeTo, 7)
                .stream()
                .map(SleepScoreResponse::from)
                .toList();

        var latestEntity = sleepScoreService.getLatestForUser(me.getId(), p)
                .orElse(null);

        SleepScoreResponse latest = (latestEntity == null) ? null : SleepScoreResponse.from(latestEntity);

        // Build UI cards from latest (if available)
        List<SleepSummaryCard> cards = new ArrayList<>();
        if (latest != null) {
            cards.add(new SleepSummaryCard(
                    "Sleep score",
                    (latest.score() == null) ? "N/A" : (latest.score() + "/100"),
                    "Ukupna procjena kvalitete sna (0–100)."
            ));

            cards.add(new SleepSummaryCard(
                    "Latencija",
                    (latest.latencyMinutes() == null) ? "N/A" : (latest.latencyMinutes() + " min"),
                    "Vrijeme do uspavljivanja. Brže uspavljivanje je obično bolje."
            ));

            cards.add(new SleepSummaryCard(
                    "Buđenja",
                    (latest.awakeningsCount() == null) ? "N/A" : String.valueOf(latest.awakeningsCount()),
                    "Broj buđenja tijekom noći. Manje buđenja znači stabilniji san."
            ));

            cards.add(new SleepSummaryCard(
                    "Kontinuitet",
                    (latest.continuityScore() == null) ? "N/A" : (latest.continuityScore() + "/100"),
                    "Koliko je san bio neprekinut. Veći kontinuitet = manje prekida."
            ));
        }

        return ResponseEntity.ok(new SleepSummaryResponse(
                p,
                rangeFrom,
                rangeTo,
                latest,
                last7,
                cards
        ));
    }
}
