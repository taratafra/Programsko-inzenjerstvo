package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.controller.dto.MessageResponse;
import Pomna_Sedmica.Mindfulnes.domain.enums.TimeOfDay;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.security.CurrentUserService;
import Pomna_Sedmica.Mindfulnes.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;
    private final CurrentUserService currentUserService;

    // ---------------------- STARA RUTA (s userId) ----------------------
    /**
     * Primjeri:
     *  GET /messages?userId=1&timeOfDay=MORNING&count=2
     *  GET /messages?userId=1&timeOfDay=AUTO&count=2&seed=2025-11-08
     */
    @GetMapping
    public ResponseEntity<MessageResponse> getMessages(@RequestParam Long userId,
                                                       @RequestParam(required = false, defaultValue = "AUTO") String timeOfDay,
                                                       @RequestParam(required = false) Integer count,
                                                       @RequestParam(required = false) String seed) {

        TimeOfDay tod = resolveTimeOfDay(timeOfDay);
        String finalSeed = (seed != null && !seed.isBlank())
                ? seed
                : MessageService.defaultDailySeed(userId, tod);

        return service.composeForUser(userId, tod, count, finalSeed)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ---------------------- NOVA RUTA (/me) ----------------------
    /**
     * Primjeri:
     *  GET /messages/me?timeOfDay=MORNING&count=2
     *  GET /messages/me?timeOfDay=AUTO&count=2&seed=2025-11-08
     *
     * Radi samo s USER tokenima (Auth0 / SPA login).
     * Ako je M2M token (nema "sub"), baca 401.
     */
    @GetMapping("/me")
    public ResponseEntity<MessageResponse> getMyMessages(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestParam(required = false, defaultValue = "AUTO") String timeOfDay,
                                                         @RequestParam(required = false) Integer count,
                                                         @RequestParam(required = false) String seed) {

        // Dohvati korisnika iz Auth0 JWT-a (ili kreiraj ako ne postoji)
        User me = currentUserService.getOrCreate(jwt);

        TimeOfDay tod = resolveTimeOfDay(timeOfDay);
        String finalSeed = (seed != null && !seed.isBlank())
                ? seed
                : MessageService.defaultDailySeed(me.getId(), tod);

        return service.composeForUser(me.getId(), tod, count, finalSeed)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ---------------------- POMOcNE METODE ----------------------
    private TimeOfDay resolveTimeOfDay(String raw) {
        if (raw == null) return guessByClock();
        String r = raw.trim().toUpperCase();
        if ("AUTO".equals(r)) return guessByClock();
        try {
            return TimeOfDay.valueOf(r);
        } catch (IllegalArgumentException ex) {
            return guessByClock(); // fallback ako dođe nešto neočekivano
        }
    }

    private TimeOfDay guessByClock() {
        LocalTime now = LocalTime.now(ZoneId.of("Europe/Zagreb"));
        int h = now.getHour();
        if (h >= 5 && h < 11) return TimeOfDay.MORNING;
        if (h >= 11 && h < 17) return TimeOfDay.MIDDAY;
        return TimeOfDay.EVENING;
    }
}
