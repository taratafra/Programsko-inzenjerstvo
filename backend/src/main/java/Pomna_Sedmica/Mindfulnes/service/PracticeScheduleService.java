package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.PracticeScheduleRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PracticeScheduleService {

    public static final String DEFAULT_TZ = "Europe/Zagreb";

    private final PracticeScheduleRepository repo;

    public List<PracticeSchedule> listForUser(Long userId) {
        return repo.findAllByUserIdOrderByStartTimeAsc(userId);
    }

    public PracticeSchedule createForUser(Long userId, PracticeScheduleRequest req) {
        validate(req);

        PracticeSchedule s = PracticeSchedule.builder()
                .userId(userId)
                .title(req.title().trim())
                .startTime(req.startTime())
                .repeatType(req.repeatType())
                .daysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()))
                .timezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim())
                .reminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore())
                .enabled(req.enabled() == null ? true : req.enabled())
                .build();

        return repo.save(s);
    }

    public PracticeSchedule updateForUser(Long userId, Long scheduleId, PracticeScheduleRequest req) {
        validate(req);

        PracticeSchedule s = repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        s.setTitle(req.title().trim());
        s.setStartTime(req.startTime());
        s.setRepeatType(req.repeatType());
        s.setDaysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()));
        s.setTimezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim());
        s.setReminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore());
        s.setEnabled(req.enabled() == null ? s.isEnabled() : req.enabled());

        return repo.save(s);
    }

    public void deleteForUser(Long userId, Long scheduleId) {
        // provjeri ownership
        repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        repo.deleteByIdAndUserId(scheduleId, userId);
    }

    private void validate(PracticeScheduleRequest req) {
        if (req.repeatType() == RepeatType.WEEKLY) {
            if (req.daysOfWeek() == null || req.daysOfWeek().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "daysOfWeek is required for WEEKLY schedules");
            }
        }
        if (req.repeatType() == RepeatType.DAILY) {
            // daysOfWeek može biti prazno/null -> ok
        }
    }

    private Set<DayOfWeek> normalizeDays(RepeatType type, Set<DayOfWeek> days) {
        if (type == RepeatType.DAILY) {
            return new HashSet<>(); // ignoriramo
        }
        return days == null ? new HashSet<>() : new HashSet<>(days);
    }
}
