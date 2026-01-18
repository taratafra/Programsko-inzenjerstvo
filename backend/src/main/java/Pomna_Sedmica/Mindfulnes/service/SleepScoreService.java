package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreWebhookRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.SleepScore;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.repository.SleepIntegrationRepository;
import Pomna_Sedmica.Mindfulnes.repository.SleepScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SleepScoreService {

    private final SleepScoreRepository scoreRepo;
    private final SleepIntegrationRepository integrationRepo;

    public Optional<SleepScore> getForUserAndDate(Long userId, LocalDate date, SleepProvider provider) {
        return scoreRepo.findByUserIdAndDateAndProvider(userId, date, provider);
    }

    public List<SleepScore> listForUser(Long userId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            // sensible default: last 30 days
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            return scoreRepo.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);
        }
        return scoreRepo.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
    }

    // NEW: latest score for a provider
    public Optional<SleepScore> getLatestForUser(Long userId, SleepProvider provider) {
        return scoreRepo.findTopByUserIdAndProviderOrderByDateDesc(userId, provider);
    }

    // NEW: list scores for a provider in range; if range missing, defaults to last N days
    public List<SleepScore> listForUserByProvider(Long userId,
                                                  SleepProvider provider,
                                                  LocalDate from,
                                                  LocalDate to,
                                                  int defaultDays) {
        LocalDate end = (to == null) ? LocalDate.now() : to;
        LocalDate start = (from == null) ? end.minusDays(defaultDays - 1L) : from;
        return scoreRepo.findByUserIdAndProviderAndDateBetweenOrderByDateAsc(userId, provider, start, end);
    }

    /**
     * Webhook-style ingest: provider tells us external_user_id and score. We resolve it to our user.
     */
    @Transactional
    public SleepScore ingestFromWebhook(SleepProvider provider, SleepScoreWebhookRequest req) {
        var integration = integrationRepo
                .findByProviderAndExternalUserId(provider, req.externalUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No integration found for provider=" + provider + " externalUserId=" + req.externalUserId()
                ));

        User user = integration.getUser();

        SleepScore score = scoreRepo
                .findByUserIdAndDateAndProvider(user.getId(), req.date(), provider)
                .orElseGet(SleepScore::new);

        score.setUser(user);
        score.setProvider(provider);
        score.setDate(req.date());
        score.setScore(req.score());
        score.setLatencyMinutes(req.latencyMinutes());
        score.setAwakeningsCount(req.awakeningsCount());
        score.setContinuityScore(req.continuityScore());
        score.setRawPayload(req.rawPayload());

        return scoreRepo.save(score);
    }
}
