package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.TrainerLinkService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trainers")
@RequiredArgsConstructor
public class TrainerLinkController {

    private final TrainerLinkService trainerLinks;
    private final UserService userService;

    // ---------------------- REAL (Auth0 USER token) ----------------------

    /**
     * User odabere primarnog trenera.
     * POST /trainers/me/primary
     * body: {"trainerId": 5}
     */
    @PostMapping("/me/primary")
    public ResponseEntity<Void> setMyPrimaryTrainer(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody Map<String, Object> body) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        Object raw = body.get("trainerId");
        if (raw == null) return ResponseEntity.badRequest().build();

        Long trainerId = Long.valueOf(String.valueOf(raw));
        trainerLinks.linkTrainer(me.getId(), trainerId, true);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /trainers/me/primary -> {"trainerId": 5} ili {"trainerId": null}
     */
    @GetMapping("/me/primary")
    public ResponseEntity<Map<String, Object>> getMyPrimaryTrainer(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        Long trainerId = trainerLinks.getPrimaryTrainerIdOrNull(me.getId());
        return ResponseEntity.ok(Map.of("trainerId", trainerId));
    }

    /**
     * Ako sam ja trener: vrati userId-e koji su spojeni na mene.
     * GET /trainers/me/users
     */
    @GetMapping("/me/users")
    public ResponseEntity<List<Long>> getUsersForMeAsTrainer(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var userIds = trainerLinks.listForTrainer(me.getId())
                .stream()
                .map(link -> link.getUserId())
                .distinct()
                .toList();

        return ResponseEntity.ok(userIds);
    }


    // ---------------------- DEV/TEST (H2/Postman bez user tokena) ----------------------
    // Ove metode postoje SAMO kad je aktivan "dev" profil.

    /**
     * POST /trainers/{userId}/primary
     * body: {"trainerId": 5}
     */
    @Profile("dev")
    @PostMapping("/{userId}/primary")
    public ResponseEntity<Void> setPrimaryDev(@PathVariable Long userId,
                                              @RequestBody Map<String, Object> body) {

        Object raw = body.get("trainerId");
        if (raw == null) return ResponseEntity.badRequest().build();

        Long trainerId = Long.valueOf(String.valueOf(raw));
        trainerLinks.linkTrainer(userId, trainerId, true);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /trainers/{trainerId}/users  (dev-only)
     */
    @Profile("dev")
    @GetMapping("/{trainerId}/users")
    public ResponseEntity<List<Long>> usersForTrainerDev(@PathVariable Long trainerId) {
        var ids = trainerLinks.listForTrainer(trainerId)
                .stream()
                .map(link -> link.getUserId())
                .distinct()
                .toList();
        return ResponseEntity.ok(ids);
    }
}
