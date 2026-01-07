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


@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    private String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new RuntimeException("JWT token is null");
        }

        String email = jwt.getSubject();

        if (email == null || !email.contains("@")) {
            email = jwt.getClaimAsString("email");
        }

        if (email == null) {
            throw new RuntimeException("Unable to extract email from JWT token");
        }

        return email;
    }

    @GetMapping
    public ResponseEntity<UserSettingsResponseDTO> getCurrentUserSettings(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO settings = userSettingsService.getUserSettings(email);
            return ResponseEntity.ok(settings);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<UserSettingsResponseDTO> updateProfileSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserSettingsRequestDTO request) {
        try {
            String email = extractEmailFromJwt(jwt);
            UserSettingsResponseDTO updatedSettings = userSettingsService.updateUserSettings(email, request);
            return ResponseEntity.ok(updatedSettings);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/first-time-reset")
    public ResponseEntity<?> resetFirstTimePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FirstTimePasswordResetRequestDTO request) {
        try {
            String email = extractEmailFromJwt(jwt);

            boolean success = userSettingsService.resetFirstTimePassword(email, request);

            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Password reset failed - user may not be local or not first login"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequestDTO request) {
        try {
            String email = extractEmailFromJwt(jwt);
            boolean success = userSettingsService.changePassword(email, request);

            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Password change failed"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }


    @GetMapping("/check-first-login")
    public ResponseEntity<?> checkFirstLogin(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = extractEmailFromJwt(jwt);
            boolean requiresReset = userSettingsService.isFirstLoginRequired(email);
            return ResponseEntity.ok().body(requiresReset);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
}