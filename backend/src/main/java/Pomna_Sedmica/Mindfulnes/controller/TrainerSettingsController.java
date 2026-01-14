package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.service.TrainerSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trainer/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class TrainerSettingsController {

    private final TrainerSettingsService trainerSettingsService;

    private String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new RuntimeException("JWT token is null");
        }

        String email = jwt.getSubject();
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
    public ResponseEntity<?> getUserSettings(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("Get trainer settings request - JWT received");
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO response = trainerSettingsService.getTrainerSettings(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting settings: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateUserSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserSettingsRequestDTO request) {
        try {
            log.info("Update trainer settings request - JWT received");
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO response = trainerSettingsService.updateTrainerSettings(email, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating settings: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-first-time-password")
    public ResponseEntity<?> resetFirstTimePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FirstTimePasswordResetRequestDTO request) {
        try {
            log.info("First-time password reset request - JWT received");
            String email = extractEmailFromJwt(jwt);
            boolean success = trainerSettingsService.resetFirstTimePassword(email, request);

            if (success) {
                log.info("First-time password reset successful for: {}", email);
                return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
            } else {
                log.warn("Password reset failed for: {} - user may not be local or not first login", email);
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Password reset failed - user may not be local or not first login"));
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
            boolean success = trainerSettingsService.changePassword(email, request);

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
            boolean requiresReset = trainerSettingsService.isFirstLoginRequired(email);
            return ResponseEntity.ok().body(requiresReset);
        } catch (RuntimeException e) {
            log.error("Error checking first login: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
}
