package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.InAppNotificationResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.InAppNotificationRepository;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationRepository notifications;
    private final UserService userService;

    // -------- REAL (/me) --------
    @GetMapping("/me")
    public ResponseEntity<List<InAppNotificationResponse>> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var list = notifications.findAllByUserIdOrderByCreatedAtDesc(me.getId())
                .stream()
                .map(InAppNotificationResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    // -------- DEV (userId u URL-u) --------
    @GetMapping("/{userId}")
    public ResponseEntity<List<InAppNotificationResponse>> notifications(@PathVariable Long userId) {
        var list = notifications.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InAppNotificationResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }
}
