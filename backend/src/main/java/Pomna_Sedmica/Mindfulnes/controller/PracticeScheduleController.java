package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.PracticeScheduleRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.PracticeScheduleResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.service.IcsExportService;
import Pomna_Sedmica.Mindfulnes.service.PracticeScheduleService;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class PracticeScheduleController {

    private final PracticeScheduleService schedules;
    private final UserService userService;
    private final IcsExportService icsExport;

    // ---------------------- REAL RUTE (/me) ----------------------

    @GetMapping("/me")
    public ResponseEntity<List<PracticeScheduleResponse>> listMySchedules(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var list = schedules.listForUser(me.getId())
                .stream()
                .map(PracticeScheduleResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PostMapping("/me")
    public ResponseEntity<PracticeScheduleResponse> createMySchedule(@AuthenticationPrincipal Jwt jwt,
                                                                     @Valid @RequestBody PracticeScheduleRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var created = schedules.createForUser(me.getId(), req);

        return ResponseEntity
                .created(URI.create("/schedules/me/" + created.getId()))
                .body(PracticeScheduleResponse.from(created));
    }

    @PutMapping("/me/{scheduleId}")
    public ResponseEntity<PracticeScheduleResponse> updateMySchedule(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long scheduleId,
                                                                     @Valid @RequestBody PracticeScheduleRequest req) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        var updated = schedules.updateForUser(me.getId(), scheduleId, req);
        return ResponseEntity.ok(PracticeScheduleResponse.from(updated));
    }

    @DeleteMapping("/me/{scheduleId}")
    public ResponseEntity<Void> deleteMySchedule(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable Long scheduleId) {
        User me = userService.getOrCreateUserFromJwt(jwt);

        schedules.deleteForUser(me.getId(), scheduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ICS export za "moje" schedule (frontend može ponuditi download).
     */
    @GetMapping(value = "/me/ics", produces = "text/calendar")
    public ResponseEntity<byte[]> exportMyIcs(@AuthenticationPrincipal Jwt jwt) {
        User me = userService.getOrCreateUserFromJwt(jwt);
        String ics = icsExport.buildIcsForUser(me.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header("Content-Disposition", "attachment; filename=\"practice-schedules.ics\"")
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------- DEV RUTE (userId u URL-u) ----------------------
    // (korisno za Postman/H2 bez user tokena)

    @GetMapping("/{userId}")
    public ResponseEntity<List<PracticeScheduleResponse>> listSchedules(@PathVariable Long userId) {
        var list = schedules.listForUser(userId)
                .stream()
                .map(PracticeScheduleResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<PracticeScheduleResponse> createSchedule(@PathVariable Long userId,
                                                                   @Valid @RequestBody PracticeScheduleRequest req) {
        var created = schedules.createForUser(userId, req);
        return ResponseEntity
                .created(URI.create("/schedules/" + userId + "/" + created.getId()))
                .body(PracticeScheduleResponse.from(created));
    }

    @PutMapping("/{userId}/{scheduleId}")
    public ResponseEntity<PracticeScheduleResponse> updateSchedule(@PathVariable Long userId,
                                                                   @PathVariable Long scheduleId,
                                                                   @Valid @RequestBody PracticeScheduleRequest req) {
        var updated = schedules.updateForUser(userId, scheduleId, req);
        return ResponseEntity.ok(PracticeScheduleResponse.from(updated));
    }

    @DeleteMapping("/{userId}/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long userId,
                                               @PathVariable Long scheduleId) {
        schedules.deleteForUser(userId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{userId}/ics", produces = "text/calendar")
    public ResponseEntity<byte[]> exportIcs(@PathVariable Long userId) {
        String ics = icsExport.buildIcsForUser(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header("Content-Disposition", "attachment; filename=\"practice-schedules-" + userId + ".ics\"")
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }
}
