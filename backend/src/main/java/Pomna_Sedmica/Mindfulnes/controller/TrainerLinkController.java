package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerLinkRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerLinkResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.TrainerLinkService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/trainers")
@RequiredArgsConstructor
public class TrainerLinkController {

    private final TrainerLinkService trainerLinks;
    private final UserService userService;

    /**
     * USER -> list trenere na koje je spojen
     * GET /trainers/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<TrainerLinkResponse>> myTrainers(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var out = trainerLinks.listMyTrainers(me.getId()).stream()
                .map(TrainerLinkResponse::from)
                .toList();

        return ResponseEntity.ok(out);
    }

    /**
     * USER -> poveži trenera (i opcionalno set primary)
     * POST /trainers/me
     */
    @PostMapping("/me")
    public ResponseEntity<TrainerLinkResponse> linkTrainer(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody TrainerLinkRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        boolean makePrimary = req.primary() != null && req.primary();
        var created = trainerLinks.linkTrainer(me.getId(), req.trainerId(), makePrimary);

        return ResponseEntity
                .created(URI.create("/trainers/me"))
                .body(TrainerLinkResponse.from(created));
    }

    /**
     * USER -> postavi primary trenera (mora već biti linkan)
     * PUT /trainers/me/primary/{trainerId}
     */
    @PutMapping("/me/primary/{trainerId}")
    public ResponseEntity<Void> setPrimary(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Long trainerId) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        trainerLinks.setPrimaryTrainer(me.getId(), trainerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * USER -> unlink trenera
     * DELETE /trainers/me/{trainerId}
     */
    @DeleteMapping("/me/{trainerId}")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long trainerId) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        trainerLinks.unlinkTrainer(me.getId(), trainerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * TRAINER (ili admin) -> list usere koji su spojeni na trenera
     * GET /trainers/{trainerId}/users
     *
     * (Za sada nema role-guard u ovom kontroleru; možete kasnije dodati u SecurityConfig.)
     */
    @GetMapping("/{trainerId}/users")
    public ResponseEntity<List<TrainerLinkResponse>> usersForTrainer(@PathVariable Long trainerId) {
        var out = trainerLinks.listUsersForTrainer(trainerId).stream()
                .map(TrainerLinkResponse::from)
                .toList();

        return ResponseEntity.ok(out);
    }
}
