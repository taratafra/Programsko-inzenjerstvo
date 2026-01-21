package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.repository.SleepIntegrationRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles the Terra authentication widget flow.
 */
@Service
@RequiredArgsConstructor
public class TerraAuthService {

    private final TerraClientService terraClientService;
    private final UserRepository userRepository;
    private final SleepIntegrationRepository integrationRepository;

    /**
     * Where Terra should redirect the browser after successful auth.
     * We default to our backend callback.
     */
    @Value("${terra.auth.success-redirect-url:http://localhost:8080/api/sleep/terra/callback/success}")
    private String authSuccessRedirectUrl;

    /**
     * Where Terra should redirect the browser after failed auth.
     */
    @Value("${terra.auth.failure-redirect-url:http://localhost:8080/api/sleep/terra/callback/failure}")
    private String authFailureRedirectUrl;

    /**
     * Where we redirect the browser AFTER we store the connection.
     * This is typically your frontend route, e.g. http://localhost:3000/sleep
     */
    @Value("${terra.frontend.return-url:http://localhost:3000/sleep}")
    private String frontendReturnUrl;

    public String generateWidgetSessionUrlForUser(User user) {
        // Use our internal user id as reference_id so we can reconcile later.
        String referenceId = String.valueOf(user.getId());
        return terraClientService.generateWidgetSession(referenceId, authSuccessRedirectUrl, authFailureRedirectUrl);
    }

    /**
     * Called by TerraCallbackController when Terra redirects back after successful auth.
     */
    @Transactional
    public String handleAuthSuccess(String terraUserId, String referenceId) {
        Long userId;
        try {
            userId = Long.parseLong(referenceId);
        } catch (NumberFormatException e) {
            // If reference_id isn't our numeric user id, just redirect with an error.
            return frontendReturnUrl + "?terra=error&reason=invalid_reference_id";
        }

        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return frontendReturnUrl + "?terra=error&reason=user_not_found";
        }

        SleepIntegration integration = integrationRepository
                .findByUserIdAndProvider(user.getId(), SleepProvider.TERRA)
                .orElseGet(SleepIntegration::new);

        integration.setUser(user);
        integration.setProvider(SleepProvider.TERRA);
        integration.setExternalUserId(terraUserId);
        integration.setConnectedAt(LocalDateTime.now());
        integrationRepository.save(integration);

        return frontendReturnUrl + "?terra=connected";
    }

    public String handleAuthFailure(String referenceId, String error) {
        StringBuilder sb = new StringBuilder(frontendReturnUrl);
        sb.append(frontendReturnUrl.contains("?") ? "&" : "?");
        sb.append("terra=error");
        if (error != null && !error.isBlank()) {
            sb.append("&reason=").append(urlEncode(error));
        }
        if (referenceId != null && !referenceId.isBlank()) {
            sb.append("&reference_id=").append(urlEncode(referenceId));
        }
        return sb.toString();
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
