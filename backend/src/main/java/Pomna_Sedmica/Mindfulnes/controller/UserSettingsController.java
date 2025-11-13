package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    private String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new RuntimeException("JWT token is null");
        }


        String email = jwt.getSubject(); // This should be the email
        log.info("JWT subject (should be email): {}", email);

        if (email == null || !email.contains("@")) {
            email = jwt.getClaimAsString("email");
            log.info("JWT email claim: {}", email);
        }

        if (email == null) {
            log.error("Unable to extract email from JWT token. Available claims: {}", jwt.getClaims());
            throw new RuntimeException("Unable to extract email from JWT token");
        }

        log.info("Successfully extracted email from JWT: {}", email);
        return email;
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
    public ResponseEntity<UserSettingsResponseDTO> updateProfileSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserSettingsRequestDTO request) {
        try {
            log.info("Updating profile settings - JWT received");
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO updatedSettings = userSettingsService.updateUserSettings(email, request);
            return ResponseEntity.ok(updatedSettings);
        } catch (RuntimeException e) {
            log.error("Error updating profile settings: {}", e.getMessage());
            return ResponseEntity.status(401).build();
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