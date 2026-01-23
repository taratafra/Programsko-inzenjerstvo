package Pomna_Sedmica.Mindfulnes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

@Slf4j
@Service
public class FitbitClientService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${fitbit.auth-url:https://www.fitbit.com/oauth2/authorize}")
    private String authUrl;

    @Value("${fitbit.api-base:https://api.fitbit.com}")
    private String apiBase;

    @Value("${fitbit.client-id:}")
    private String clientId;

    @Value("${fitbit.client-secret:}")
    private String clientSecret;

    @Value("${fitbit.redirect-uri:}")
    private String redirectUri;

    @Value("${fitbit.scopes:sleep}")
    private String scopes;

    // ✅ DEV-only flag from application.yml (dev profile)
    @Value("${fitbit.mock.enabled:false}")
    private boolean mockEnabled;

    public FitbitClientService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String buildAuthorizeUrl(String state) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Fitbit client-id missing (fitbit.client-id).");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("Fitbit redirect-uri missing (fitbit.redirect-uri).");
        }

        String url = UriComponentsBuilder.fromHttpUrl(authUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scopes)
                .queryParam("state", state)
                .build()
                .toUriString();

        log.info("[FITBIT] authorizeUrl={}", url);
        return url;
    }

    public JsonNode exchangeCodeForToken(String code) {
        requireClientSecrets();

        String tokenUrl = apiBase + "/oauth2/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", basicAuthHeader(clientId, clientSecret))
                .bodyValue("client_id=" + encode(clientId) +
                        "&grant_type=authorization_code" +
                        "&code=" + encode(code) +
                        "&redirect_uri=" + encode(redirectUri))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode refreshAccessToken(String refreshToken) {
        requireClientSecrets();

        String tokenUrl = apiBase + "/oauth2/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", basicAuthHeader(clientId, clientSecret))
                .bodyValue("grant_type=refresh_token&refresh_token=" + encode(refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode getSleepByDateRange(String accessToken, LocalDate from, LocalDate to) {
        // ✅ DEV MOCK MODE
        if (mockEnabled) {
            log.warn("[FITBIT] MOCK MODE enabled (dev profile) -> returning fake sleep data {}..{}", from, to);
            return buildMockSleepPayload(from, to);
        }

        String url = apiBase + "/1.2/user/-/sleep/date/" + from + "/" + to + ".json";

        return webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    // ================== MOCK ==================

    private JsonNode buildMockSleepPayload(LocalDate from, LocalDate to) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode sleepArr = objectMapper.createArrayNode();

        // Generate up to 7 days (your controller uses last 7 days)
        LocalDate d = to;
        int i = 0;
        while (!d.isBefore(from) && i < 7) {
            ObjectNode entry = objectMapper.createObjectNode();

            int efficiency = 55 + (i * 5); // 55,60,65,...
            int latency = 8 + i;           // 8..14
            int wakeCount = 1 + (i % 4);   // 1..4

            entry.put("dateOfSleep", d.toString());
            entry.put("efficiency", Math.min(efficiency, 95));
            entry.put("minutesAsleep", 360 + i * 15);
            entry.put("minutesToFallAsleep", latency);

            // levels.summary.wake.count
            ObjectNode levels = objectMapper.createObjectNode();
            ObjectNode summary = objectMapper.createObjectNode();
            ObjectNode wake = objectMapper.createObjectNode();
            wake.put("count", wakeCount);
            summary.set("wake", wake);
            levels.set("summary", summary);
            entry.set("levels", levels);

            sleepArr.add(entry);

            d = d.minusDays(1);
            i++;
        }

        root.set("sleep", sleepArr);
        root.set("summary", objectMapper.createObjectNode());
        return root;
    }

    // ================== UTIL ==================

    private void requireClientSecrets() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Fitbit client-id missing (fitbit.client-id).");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Fitbit client-secret missing (fitbit.client-secret).");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("Fitbit redirect-uri missing (fitbit.redirect-uri).");
        }
    }

    private String basicAuthHeader(String id, String secret) {
        String raw = id + ":" + secret;
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
