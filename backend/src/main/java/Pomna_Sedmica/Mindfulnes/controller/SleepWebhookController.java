package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreWebhookRequest;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.service.SleepScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Generic webhook receiver for sleep scores.
 *
 * Terra/Sahha (and similar) will POST events to your backend when new sleep data is ready.
 * This controller provides a simple, provider-agnostic webhook for the project.
 */
@RestController
@RequestMapping("/api/webhooks/sleep")
@RequiredArgsConstructor
public class SleepWebhookController {

    private final SleepScoreService sleepScoreService;

    /**
     * Simple shared secret check for demo usage.
     * Configure with SLEEP_WEBHOOK_SECRET env var.
     */
    @Value("${sleep.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/{provider}")
    public ResponseEntity<SleepScoreResponse> ingest(@PathVariable SleepProvider provider,
                                                     @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
                                                     @Valid @RequestBody SleepScoreWebhookRequest req) {

        // If secret configured, require it.
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (secret == null || !webhookSecret.equals(secret)) {
                return ResponseEntity.status(401).build();
            }
        }

        var saved = sleepScoreService.ingestFromWebhook(provider, req);
        return ResponseEntity.ok(SleepScoreResponse.from(saved));
    }
}
