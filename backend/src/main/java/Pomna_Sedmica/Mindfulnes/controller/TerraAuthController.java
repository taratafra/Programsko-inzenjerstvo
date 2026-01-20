package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.TerraWidgetSessionResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.TerraAuthService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Terra connect flow:
 *  - Frontend calls this endpoint to get a Widget URL
 *  - Frontend opens the returned URL (in-app browser / normal browser)
 *  - Terra redirects to our callback endpoints (configured in TerraAuthService)
 */
@RestController
@RequestMapping("/api/sleep/terra")
@RequiredArgsConstructor
public class TerraAuthController {

    private final UserService userService;
    private final TerraAuthService terraAuthService;

    /**
     * Returns a Terra widget URL that the client should open.
     */
    @GetMapping("/widget-session")
    public ResponseEntity<TerraWidgetSessionResponse> createWidgetSession(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        String url = terraAuthService.generateWidgetSessionUrlForUser(me);
        return ResponseEntity.ok(new TerraWidgetSessionResponse(url));
    }
}
