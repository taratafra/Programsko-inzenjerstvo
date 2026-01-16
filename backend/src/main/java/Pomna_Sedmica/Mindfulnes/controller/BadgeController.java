package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.BadgeAwardResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.BadgeAwardRepository;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeAwardRepository badges;
    private final UserService userService;

    // ---------- /me (Auth0/JWT) ----------
    @GetMapping("/me")
    public ResponseEntity<List<BadgeAwardResponse>> myBadges(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var list = badges.findAllByUserIdOrderByAwardedAtDesc(me.getId())
                .stream()
                .map(BadgeAwardResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    // ---------- DEV/TEST ----------
    @GetMapping("/{userId}")
    public ResponseEntity<List<BadgeAwardResponse>> badgesForUser(@PathVariable Long userId) {
        var list = badges.findAllByUserIdOrderByAwardedAtDesc(userId)
                .stream()
                .map(BadgeAwardResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }
}
