package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.entity.SleepScore;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.repository.SleepIntegrationRepository;
import Pomna_Sedmica.Mindfulnes.repository.SleepScoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitSleepSyncService {

    private final FitbitClientService fitbitClient;
    private final SleepIntegrationRepository integrationRepo;
    private final SleepScoreRepository scoreRepo;

    // Frontend koristi provider=TERRA (legacy alias za Fitbit)
    private static final SleepProvider LEGACY_PROVIDER = SleepProvider.TERRA;

    @Transactional
    public void syncLast7DaysIfConnected(Long userId) {
        SleepIntegration integ = integrationRepo
                .findByUserIdAndProvider(userId, LEGACY_PROVIDER)
                .orElse(null);

        if (integ == null) return;

        ensureValidAccessToken(integ);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);

        JsonNode payload = fitbitClient.getSleepByDateRange(
                integ.getAccessToken(), from, to
        );

        if (payload == null || payload.get("sleep") == null) {
            log.warn("[FITBIT] No sleep data returned");
            return;
        }

        for (JsonNode entry : payload.get("sleep")) {
            LocalDate date = parseDate(entry, "dateOfSleep");
            if (date == null) continue;

            Integer efficiency = intVal(entry, "efficiency");
            Integer minutesAsleep = intVal(entry, "minutesAsleep");

            // === SCORE ===
            Integer score = null;
            if (efficiency != null) {
                score = clamp(efficiency, 0, 100);
            } else if (minutesAsleep != null) {
                score = clamp(
                        (int) Math.round((minutesAsleep / 480.0) * 100.0),
                        0, 100
                );
            }

            if (score == null) continue;

            SleepScore entity = scoreRepo
                    .findByUserIdAndDateAndProvider(userId, date, LEGACY_PROVIDER)
                    .orElseGet(SleepScore::new);

            entity.setUser(integ.getUser());
            entity.setProvider(LEGACY_PROVIDER);
            entity.setDate(date);
            entity.setScore(score);

            // === SLEEP LATENCY ===
            entity.setLatencyMinutes(
                    intVal(entry, "minutesToFallAsleep")
            );

            // === BUĐENJA ===
            entity.setAwakeningsCount(
                    intVal(entry, "levels", "summary", "wake", "count")
            );

            // === SLEEP EFFICIENCY ===
            entity.setContinuityScore(
                    efficiency != null ? clamp(efficiency, 0, 100) : null
            );

            // Raw payload (debug)
            entity.setRawPayload(entry.toString());

            scoreRepo.save(entity);
        }
    }

    // ========================= HELPERS =========================

    private void ensureValidAccessToken(SleepIntegration integ) {
        if (integ.getTokenExpiresAt() == null) return;

        if (integ.getTokenExpiresAt()
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2))) {

            if (integ.getRefreshToken() == null) return;

            JsonNode refreshed = fitbitClient
                    .refreshAccessToken(integ.getRefreshToken());

            if (refreshed == null) return;

            integ.setAccessToken(text(refreshed, "access_token"));
            integ.setRefreshToken(text(refreshed, "refresh_token"));

            Long expiresIn = longVal(refreshed, "expires_in");
            if (expiresIn != null) {
                integ.setTokenExpiresAt(
                        LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn)
                );
            }

            integrationRepo.save(integ);
        }
    }

    private LocalDate parseDate(JsonNode node, String field) {
        try {
            return node.has(field)
                    ? LocalDate.parse(node.get(field).asText())
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer intVal(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asInt()
                : null;
    }

    private Integer intVal(JsonNode node, String f1, String f2, String f3, String f4) {
        JsonNode current = node;
        for (String f : new String[]{f1, f2, f3, f4}) {
            if (current == null || !current.has(f)) return null;
            current = current.get(f);
        }
        return current.isNull() ? null : current.asInt();
    }

    private String text(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText(null) : null;
    }

    private Long longVal(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : null;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
