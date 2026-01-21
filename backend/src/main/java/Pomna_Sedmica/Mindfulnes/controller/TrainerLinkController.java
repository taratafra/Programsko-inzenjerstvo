package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.UserTrainer;
import Pomna_Sedmica.Mindfulnes.service.TrainerLinkService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainers")
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

        // Handle null trainerId properly
        Map<String, Object> response = new HashMap<>();
        response.put("trainerId", trainerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all trainers the user is subscribed to
     * GET /trainers/me/subscriptions
     */
    @GetMapping("/me/subscriptions")
    public ResponseEntity<List<Long>> getMySubscriptions(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var trainerIds = trainerLinks.listMyTrainers(me.getId())
                .stream()
                .map(UserTrainer::getTrainerId)
                .distinct()
                .toList();

        return ResponseEntity.ok(trainerIds);
    }

    /**
     * Subscribe to a trainer (without making them primary)
     * POST /trainers/me/subscribe
     * body: {"trainerId": 5}
     */
//    @PostMapping("/me/subscribe")
//    public ResponseEntity<Void> subscribeToTrainer(@AuthenticationPrincipal Jwt jwt,
//                                                   @RequestBody Map<String, Object> body) {
//        User me = userService.getOrCreateUserFromJwt(jwt);
//
//        Object raw = body.get("trainerId");
//        if (raw == null) return ResponseEntity.badRequest().build();
//
//        Long trainerId = Long.valueOf(String.valueOf(raw));
//        trainerLinks.linkTrainer(me.getId(), trainerId, false); // false = not primary
//
//        return ResponseEntity.noContent().build();
//    }

    @PostMapping("/me/subscribe")
    public ResponseEntity<Void> subscribeToTrainer(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody Map<String, Object> body) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        Object raw = body.get("trainerId");
        if (raw == null) {
            return ResponseEntity.badRequest().build();
        }

        Long trainerId = Long.valueOf(String.valueOf(raw));

        // 🔒 BLOCK self-subscription
        if (me.getId().equals(trainerId)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        trainerLinks.linkTrainer(me.getId(), trainerId, false); // false = not primary

        return ResponseEntity.noContent().build();
    }


    /**
     * Unsubscribe from a trainer
     * DELETE /trainers/me/{trainerId}
     */
    @DeleteMapping("/me/{trainerId}")
    public ResponseEntity<Void> unsubscribeFromTrainer(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable Long trainerId) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        trainerLinks.unlinkTrainer(me.getId(), trainerId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Ako sam ja trener: vrati userId-e koji su spojeni na mene.
     * GET /trainers/me/users
     */
    @GetMapping("/me/users")
    public ResponseEntity<List<Long>> getUsersForMeAsTrainer(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var userIds = trainerLinks.listUsersForTrainer(me.getId())
                .stream()
                .map(link -> link.getUserId())
                .distinct()
                .toList();

        return ResponseEntity.ok(userIds);
    }
/**
 * Get full user details for all clients subscribed to me as a trainer
 * GET /trainers/me/clients
 */
@GetMapping("/me/clients")
public ResponseEntity<List<User>> getMyClientsDetails(@AuthenticationPrincipal Jwt jwt) {
    User me = userService.getOrCreateUserFromJwt(jwt);

    var clientIds = trainerLinks.listUsersForTrainer(me.getId())
            .stream()
            .map(UserTrainer::getUserId)
            .distinct()
            .toList();

    // Fetch full user details for each client
    var clients = clientIds.stream()
            .map(userId -> userService.getUserById(userId).orElse(null))
            .filter(user -> user != null)
            .toList();

    return ResponseEntity.ok(clients);
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
        var ids = trainerLinks.listUsersForTrainer(trainerId)
                .stream()
                .map(link -> link.getUserId())
                .distinct()
                .toList();
        return ResponseEntity.ok(ids);
    }
}
