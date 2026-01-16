package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.MoodCheckInRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.MoodCheckInResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.MoodCheckInService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mood-checkins")
@RequiredArgsConstructor
public class MoodCheckInController {

    private final MoodCheckInService service;
    private final UserService userService;

    // ------------------------- /me (Auth0 / JWT) -------------------------

    @PostMapping("/me")
    public ResponseEntity<MoodCheckInResponse> upsertMyCheckIn(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody MoodCheckInRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        var saved = service.upsertForUser(me.getId(), req);
        return ResponseEntity
                .created(URI.create("/mood-checkins/me"))
                .body(MoodCheckInResponse.from(saved));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MoodCheckInResponse>> listMyCheckIns(@AuthenticationPrincipal Jwt jwt,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                    LocalDate from,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                    LocalDate to) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        var list = service.listForUser(me.getId(), from, to).stream().map(MoodCheckInResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me/{date}")
    public ResponseEntity<MoodCheckInResponse> getMyCheckIn(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                            LocalDate date) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        return ResponseEntity.ok(MoodCheckInResponse.from(service.getForUserAndDate(me.getId(), date)));
    }

    // ------------------------- DEV/TEST rute s userId -------------------------

    @PostMapping("/{userId}")
    public ResponseEntity<MoodCheckInResponse> upsertCheckIn(@PathVariable Long userId,
                                                             @Valid @RequestBody MoodCheckInRequest req) {
        var saved = service.upsertForUser(userId, req);
        return ResponseEntity.created(URI.create("/mood-checkins/" + userId)).body(MoodCheckInResponse.from(saved));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<MoodCheckInResponse>> listCheckIns(@PathVariable Long userId,
                                                                  @RequestParam(required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                  LocalDate from,
                                                                  @RequestParam(required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                  LocalDate to) {
        var list = service.listForUser(userId, from, to).stream().map(MoodCheckInResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{userId}/{date}")
    public ResponseEntity<MoodCheckInResponse> getCheckIn(@PathVariable Long userId,
                                                          @PathVariable
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                          LocalDate date) {
        return ResponseEntity.ok(MoodCheckInResponse.from(service.getForUserAndDate(userId, date)));
    }
}
