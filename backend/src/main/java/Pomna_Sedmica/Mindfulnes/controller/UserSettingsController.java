package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth0.domain}")
    private String auth0Domain;

    private String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new RuntimeException("JWT token is null");
        }

        log.info("=== JWT DEBUG INFO ===");
        log.info("All JWT claims: {}", jwt.getClaims());

        String email = null;

        // Try to get email from JWT claims first
        email = jwt.getClaimAsString("email");
        if (email != null && email.contains("@")) {
            log.info("Email found in JWT 'email' claim: {}", email);
            return email;
        }

        // Check if subject is an email
        email = jwt.getSubject();
        if (email != null && email.contains("@")) {
            log.info("Email found in JWT subject: {}", email);
            return email;
        }

        // For Auth0 tokens without email claim, fetch from userinfo endpoint
        String issuer = jwt.getIssuer().toString();
        if (issuer.contains("auth0.com")) {
            log.info("Auth0 token detected, fetching email from userinfo endpoint");
            return fetchEmailFromAuth0Userinfo(jwt);
        }

        log.error("Unable to extract email from JWT token. Available claims: {}", jwt.getClaims());
        throw new RuntimeException("Unable to extract email from JWT token");
    }

    private String fetchEmailFromAuth0Userinfo(Jwt jwt) {
        try {
            String userinfoUrl = auth0Domain.endsWith("/")
                    ? auth0Domain + "userinfo"
                    : auth0Domain + "/userinfo";

            log.info("Fetching user info from: {}", userinfoUrl);

            // Create headers with the access token
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            // Call Auth0 userinfo endpoint
            ResponseEntity<Map> response = restTemplate.exchange(
                    userinfoUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> userInfo = response.getBody();
            log.info("Auth0 userinfo response: {}", userInfo);

            if (userInfo != null && userInfo.containsKey("email")) {
                String email = (String) userInfo.get("email");
                log.info("Email fetched from Auth0 userinfo: {}", email);
                return email;
            }

            throw new RuntimeException("No email in Auth0 userinfo response");

        } catch (Exception e) {
            log.error("Error fetching email from Auth0 userinfo: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch email from Auth0: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<UserSettingsResponseDTO> getCurrentUserSettings(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("Getting user settings - JWT received");
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO settings = userSettingsService.getUserSettings(email);
            return ResponseEntity.ok(settings);
        } catch (RuntimeException e) {
            log.error("Error getting user settings: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfileSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserSettingsRequestDTO request) {
        try {
            log.info("=== UPDATE PROFILE REQUEST ===");
            String email = extractEmailFromJwt(jwt);
            log.info("Extracted email: {}", email);

            UserSettingsResponseDTO updatedSettings = userSettingsService.updateUserSettings(email, request);
            log.info("Profile updated successfully for: {}", email);

            return ResponseEntity.ok(updatedSettings);
        } catch (RuntimeException e) {
            log.error("Error updating profile settings: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating profile settings: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/first-time-reset")
    public ResponseEntity<?> resetFirstTimePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FirstTimePasswordResetRequestDTO request) {
        try {
            log.info("First-time password reset request - JWT received");
            String email = extractEmailFromJwt(jwt);
            log.info("Attempting password reset for email: {}", email);

            boolean success = userSettingsService.resetFirstTimePassword(email, request);

            if (success) {
                log.info("Password reset successful for: {}", email);
                return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
            } else {
                log.warn("Password reset failed for: {} - user may not be local or not first login", email);
                return ResponseEntity.badRequest().body(Map.of("error", "Password reset failed - user may not be local or not first login"));
            }
        } catch (RuntimeException e) {
            log.error("Error in first-time password reset: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequestDTO request) {
        try {
            log.info("Change password request - JWT received");
            String email = extractEmailFromJwt(jwt);
            boolean success = userSettingsService.changePassword(email, request);

            if (success) {
                log.info("Password change successful for: {}", email);
                return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
            } else {
                log.warn("Password change failed for: {}", email);
                return ResponseEntity.badRequest().body(Map.of("error", "Password change failed"));
            }
        } catch (RuntimeException e) {
            log.error("Error in change password: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/check-first-login")
    public ResponseEntity<?> checkFirstLogin(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("Check first login request - JWT received");
            String email = extractEmailFromJwt(jwt);
            boolean requiresReset = userSettingsService.isFirstLoginRequired(email);
            return ResponseEntity.ok().body(requiresReset);
        } catch (RuntimeException e) {
            log.error("Error checking first login: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
}