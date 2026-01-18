package Pomna_Sedmica.Mindfulnes.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Generic webhook payload you can use for demo/testing.
 *
 * Each provider has its own schema; in a real integration you would create
 * per-provider DTOs and map them into this normalized shape.
 */
public record SleepScoreWebhookRequest(
        @NotBlank @JsonProperty("external_user_id") String externalUserId,
        @NotNull LocalDate date,
        @NotNull @Min(0) @Max(100) Integer score,
        @JsonProperty("latency_minutes") Integer latencyMinutes,
        @JsonProperty("awakenings_count") Integer awakeningsCount,
        @JsonProperty("continuity_score") Integer continuityScore,
        /** Optional raw payload for debugging (stringified JSON). */
        @JsonProperty("raw_payload") String rawPayload
) {}
