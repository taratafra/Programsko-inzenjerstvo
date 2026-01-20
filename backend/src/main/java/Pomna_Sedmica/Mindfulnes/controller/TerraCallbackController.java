package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.service.TerraAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Terra redirects the user to these endpoints after the widget flow.
 *
 * IMPORTANT: these endpoints must be publicly accessible (no auth) because the browser
 * comes from Terra, not your app.
 */
@RestController
@RequestMapping("/api/sleep/terra/callback")
@RequiredArgsConstructor
public class TerraCallbackController {

    private final TerraAuthService terraAuthService;

    @GetMapping("/success")
    public ResponseEntity<Void> onSuccess(@RequestParam("user_id") String terraUserId,
                                          @RequestParam("reference_id") String referenceId) {
        // Persist the integration and redirect user back to your frontend.
        String redirect = terraAuthService.handleAuthSuccess(terraUserId, referenceId);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirect)
                .build();
    }

    @GetMapping("/failure")
    public ResponseEntity<Void> onFailure(@RequestParam(value = "reference_id", required = false) String referenceId,
                                          @RequestParam(value = "error", required = false) String error) {
        String redirect = terraAuthService.handleAuthFailure(referenceId, error);
        return ResponseEntity.status(302)
                .location(URI.create(redirect))
                .build();
    }
}
