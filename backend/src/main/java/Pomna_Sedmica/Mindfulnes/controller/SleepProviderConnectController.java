package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.AuthUrlResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.FitbitAuthService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * LEGACY ROUTES FOR FRONTEND COMPATIBILITY:
 * Smartwatch.jsx calls /api/sleep/terra/widget-session and expects { url }
 *
 * Internally we use Fitbit OAuth2 instead of Terra.
 */
@RestController
@RequestMapping("/api/sleep/terra")
@RequiredArgsConstructor
public class SleepProviderConnectController {

    private final UserService userService;
    private final FitbitAuthService fitbitAuthService;

    @GetMapping("/widget-session")
    public ResponseEntity<AuthUrlResponse> createWidgetSession(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        String url = fitbitAuthService.buildAuthorizeUrlForUser(me);
        return ResponseEntity.ok(new AuthUrlResponse(url));
    }

    /**
     * Fitbit OAuth redirect URI should point HERE (success endpoint).
     * Fitbit returns: ?code=...&state=... or ?error=...
     */
    @GetMapping("/callback/success")
    public ResponseEntity<Void> callbackSuccess(@RequestParam(value = "code", required = false) String code,
                                                @RequestParam(value = "state", required = false) String state,
                                                @RequestParam(value = "error", required = false) String error) {

        String redirect = fitbitAuthService.handleCallback(code, state, error);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirect)
                .build();
    }

    /**
     * Kept only because frontend/old flow expects it to exist.
     * We just redirect to frontend with an error marker.
     */
    @GetMapping("/callback/failure")
    public ResponseEntity<Void> callbackFailure(@RequestParam(value = "error", required = false) String error) {
        String redirect = "http://localhost:3000/sleep?terra=error&reason=" + (error == null ? "unknown" : error);
        return ResponseEntity.status(302)
                .location(URI.create(redirect))
                .build();
    }
}
