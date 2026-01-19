package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreWebhookRequest;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.service.SleepScoreService;
import Pomna_Sedmica.Mindfulnes.service.TerraSleepMapper;
import Pomna_Sedmica.Mindfulnes.util.TerraWebhookVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Receives Terra webhooks.
 *
 * Terra signs webhook payloads using the `terra-signature` header.
 * This endpoint must receive the RAW request body to verify the signature.
 */
@RestController
@RequestMapping("/api/webhooks/terra")
@RequiredArgsConstructor
public class TerraWebhookController {

    private final ObjectMapper objectMapper;
    private final TerraSleepMapper terraSleepMapper;
    private final SleepScoreService sleepScoreService;

    @Value("${terra.signing-secret:}")
    private String signingSecret;

    /** Time tolerance for signatures (seconds). */
    @Value("${terra.signature-tolerance-seconds:300}")
    private long toleranceSeconds;

    @PostMapping(value = "/sleep", consumes = "application/json")
    public ResponseEntity<String> receiveSleepWebhook(
            @RequestHeader(value = "terra-signature", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        try {
            if (!TerraWebhookVerifier.verify(signature, rawBody, signingSecret, toleranceSeconds)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Terra signature");
            }

            JsonNode event = objectMapper.readTree(rawBody);
            List<SleepScoreWebhookRequest> requests = terraSleepMapper.toSleepScoreWebhookRequests(event);

            // Acknowledge quickly, then ingest.
            for (SleepScoreWebhookRequest req : requests) {
                sleepScoreService.ingestFromWebhook(SleepProvider.TERRA, req);
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process webhook: " + e.getMessage());
        }
    }
}
