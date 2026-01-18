package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepIntegrationConnectRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.SleepIntegrationStatusResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.service.SleepIntegrationService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for connecting/disconnecting external sleep providers.
 *
 * NOTE: In a real production integration you'd usually do OAuth redirects + code exchange.
 * For the project, this controller keeps it simple.
 */
@RestController
@RequestMapping("/api/sleep/integrations")
@RequiredArgsConstructor
public class SleepIntegrationController {

    private final UserService userService;
    private final SleepIntegrationService integrationService;

    @GetMapping("/me/{provider}")
    public ResponseEntity<SleepIntegrationStatusResponse> status(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable SleepProvider provider) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        var opt = integrationService.get(me, provider);
        return ResponseEntity.ok(SleepIntegrationStatusResponse.from(provider, opt.orElse(null)));
    }

    @PostMapping("/me/{provider}")
    public ResponseEntity<SleepIntegrationStatusResponse> connect(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable SleepProvider provider,
                                                                  @Valid @RequestBody SleepIntegrationConnectRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        var saved = integrationService.connect(me, provider, req);
        return ResponseEntity.ok(SleepIntegrationStatusResponse.from(provider, saved));
    }

    @DeleteMapping("/me/{provider}")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable SleepProvider provider) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        integrationService.disconnect(me, provider);
        return ResponseEntity.noContent().build();
    }
}
