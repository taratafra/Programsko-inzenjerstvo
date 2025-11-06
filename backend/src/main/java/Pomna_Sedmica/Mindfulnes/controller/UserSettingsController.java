package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.UpdateUserSettingsRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserSettingsResponseDTO;
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
     * Get user settings by user ID (for admin purposes)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserSettingsResponseDTO> getUserSettingsById(@PathVariable Long userId) {
        UserSettingsResponseDTO settings = userSettingsService.getUserSettings(userId);
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
     * Check if current user requires first-time actions
     */
    @GetMapping("/check-first-login")
    public ResponseEntity<?> checkFirstLogin(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        boolean requiresFirstLogin = userSettingsService.isFirstLoginRequired(email);
        return ResponseEntity.ok().body(requiresFirstLogin);
    }

    /**
     * Mark first login as completed
     */
    @PostMapping("/complete-first-login")
    public ResponseEntity<?> completeFirstLogin(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        userSettingsService.completeFirstLogin(email);
        return ResponseEntity.ok().body("First login completed");
    }
}