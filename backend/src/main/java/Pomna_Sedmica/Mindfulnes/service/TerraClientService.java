package Pomna_Sedmica.Mindfulnes.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Minimal Terra API client.
 *
 * We only need /auth/generateWidgetSession for this project. Terra then pushes data to our webhook.
 */
@Service
@RequiredArgsConstructor
public class TerraClientService {

    private final WebClient.Builder webClientBuilder;

    @Value("${terra.base-url:https://api.tryterra.co/v2}")
    private String baseUrl;

    @Value("${terra.dev-id:}")
    private String devId;

    @Value("${terra.api-key:}")
    private String apiKey;

    public String generateWidgetSession(String referenceId,
                                        String authSuccessRedirectUrl,
                                        String authFailureRedirectUrl) {
        if (devId == null || devId.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Terra credentials missing. Set terra.dev-id and terra.api-key.");
        }

        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        JsonNode res = client.post()
                .uri("/auth/generateWidgetSession")
                .contentType(MediaType.APPLICATION_JSON)
                .header("dev-id", devId)
                .header("x-api-key", apiKey)
                .bodyValue(Map.of(
                        "language", "en",
                        "reference_id", referenceId,
                        "auth_success_redirect_url", authSuccessRedirectUrl,
                        "auth_failure_redirect_url", authFailureRedirectUrl
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (res == null || res.get("url") == null || res.get("url").asText().isBlank()) {
            throw new IllegalStateException("Terra did not return a widget session URL.");
        }
        return res.get("url").asText();
    }
}
