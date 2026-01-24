package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.repository.SleepIntegrationRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitAuthService {

    private final FitbitClientService fitbitClient;
    private final UserRepository userRepository;
    private final SleepIntegrationRepository integrationRepository;

    @Value("${fitbit.frontend.return-url:http://localhost:3000/sleep}")
    private String frontendReturnUrl;

    /**
     * IMPORTANT:
     * Frontend uses provider "TERRA" everywhere.
     * To keep compatibility without changing Smartwatch.jsx,
     * we store provider as TERRA (legacy alias) but it's actually Fitbit.
     */
    private static final SleepProvider LEGACY_PROVIDER = SleepProvider.TERRA;

    public String buildAuthorizeUrlForUser(User user) {
        String state = String.valueOf(user.getId()); // reference_id equivalent
        return fitbitClient.buildAuthorizeUrl(state);
    }

    @Transactional
    public String handleCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            log.warn("[FITBIT] callback error={}, state={}", error, state);
            return frontendReturnUrl + "?terra=error&reason=" + urlEncode(error);
        }

        Long userId;
        try {
            userId = Long.parseLong(state);
        } catch (Exception e) {
            return frontendReturnUrl + "?terra=error&reason=invalid_state";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return frontendReturnUrl + "?terra=error&reason=user_not_found";
        }

        JsonNode token = fitbitClient.exchangeCodeForToken(code);
        if (token == null) {
            return frontendReturnUrl + "?terra=error&reason=token_exchange_failed";
        }

        String accessToken = text(token, "access_token");
        String refreshToken = text(token, "refresh_token");
        String fitbitUserId = text(token, "user_id");
        String scope = text(token, "scope");
        String tokenType = text(token, "token_type");
        long expiresIn = longVal(token, "expires_in", 0);

        if (accessToken == null || accessToken.isBlank() || fitbitUserId == null || fitbitUserId.isBlank()) {
            return frontendReturnUrl + "?terra=error&reason=invalid_token_response";
        }

        SleepIntegration integration = integrationRepository
                .findByUserIdAndProvider(user.getId(), LEGACY_PROVIDER)
                .orElseGet(SleepIntegration::new);

        integration.setUser(user);
        integration.setProvider(LEGACY_PROVIDER);
        integration.setExternalUserId(fitbitUserId);
        integration.setAccessToken(accessToken);
        integration.setRefreshToken(refreshToken);
        integration.setTokenType(tokenType);
        integration.setScope(scope);

        if (expiresIn > 0) {
            integration.setTokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn));
        } else {
            integration.setTokenExpiresAt(null);
        }

        integration.setConnectedAt(LocalDateTime.now());
        integrationRepository.save(integration);

        return frontendReturnUrl + "?terra=connected"; // keep old param so ništa ne pukne
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static long longVal(JsonNode node, String field, long def) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? def : v.asLong(def);
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
