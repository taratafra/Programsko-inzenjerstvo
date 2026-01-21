package Pomna_Sedmica.Mindfulnes.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Minimal payload to connect a user to an external sleep provider.
 *
 * In a real OAuth integration, the backend would exchange an auth code for tokens.
 * For the demo/project, we allow the client to send provider-specific identifiers.
 */
public record SleepIntegrationConnectRequest(
        @NotBlank String externalUserId,
        String accessToken
) {}
