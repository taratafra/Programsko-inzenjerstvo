package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.ChangePasswordRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.FirstTimePasswordResetRequestDTO;
import Pomna_Sedmica.Mindfulnes.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/first-time-reset")
    public ResponseEntity<?> resetFirstTimePassword(@RequestBody FirstTimePasswordResetRequestDTO request) {
        boolean success = passwordResetService.resetFirstTimePassword(request);

        if (success) {
            return ResponseEntity.ok().body("Password reset successfully");
        } else {
            return ResponseEntity.badRequest().body("Password reset failed");
        }
    }

    @PostMapping("/change")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDTO request) {
        boolean success = passwordResetService.changePassword(request);

        if (success) {
            return ResponseEntity.ok().body("Password changed successfully");
        } else {
            return ResponseEntity.badRequest().body("Password change failed");
        }
    }

    @GetMapping("/check-first-login/{email}")
    public ResponseEntity<?> checkFirstLogin(@PathVariable String email) {
        boolean requiresReset = passwordResetService.isFirstLoginRequired(email);
        return ResponseEntity.ok().body(requiresReset);
    }
}