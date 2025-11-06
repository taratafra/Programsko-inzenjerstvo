package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * Get current user's settings
     */
    @GetMapping
    public ResponseEntity<UserSettingsResponseDTO> getCurrentUserSettings(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        UserSettingsResponseDTO settings = userSettingsService.getUserSettings(email);
        return ResponseEntity.ok(settings);
    }

    /**
     * Update user profile settings
     */
    @PutMapping("/profile")
    public ResponseEntity<UserSettingsResponseDTO> updateProfileSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserSettingsRequestDTO request) {
        String email = jwt.getClaimAsString("email");
        UserSettingsResponseDTO updatedSettings = userSettingsService.updateUserSettings(email, request);
        return ResponseEntity.ok(updatedSettings);
    }

    /**
     * Reset password for first-time login (local users only)
     */
    @PostMapping("/first-time-reset")
    public ResponseEntity<?> resetFirstTimePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FirstTimePasswordResetRequestDTO request) {
        String email = jwt.getClaimAsString("email");
        boolean success = userSettingsService.resetFirstTimePassword(email, request);

        if (success) {
            return ResponseEntity.ok().body("Password reset successfully");
        } else {
            return ResponseEntity.badRequest().body("Password reset failed - user may not be local or not first login");
        }
    }

    /**
     * Change password for existing local users
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequestDTO request) {
        String email = jwt.getClaimAsString("email");
        boolean success = userSettingsService.changePassword(email, request);

        if (success) {
            return ResponseEntity.ok().body("Password changed successfully");
        } else {
            return ResponseEntity.badRequest().body("Password change failed");
        }
    }

    /**
     * Check if current user requires first-time password reset
     */
    @GetMapping("/check-first-login")
    public ResponseEntity<?> checkFirstLogin(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        boolean requiresReset = userSettingsService.isFirstLoginRequired(email);
        return ResponseEntity.ok().body(requiresReset);
    }
}