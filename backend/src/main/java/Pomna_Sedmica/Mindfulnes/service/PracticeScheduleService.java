package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.PracticeScheduleRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserTrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PracticeScheduleService {

    public static final String DEFAULT_TZ = "Europe/Sarajevo";

    private final PracticeScheduleRepository repo;
    private final UserTrainerRepository userTrainerRepo;

    public PracticeSchedule createForUser(Long userId, PracticeScheduleRequest req) {
        validate(req);
        enforceHasTrainer(userId);

        PracticeSchedule s = PracticeSchedule.builder()
                .userId(userId)
                .title(req.title().trim())
                .startTime(req.startTime())
                .repeatType(req.repeatType())
                .daysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()))
                .date(normalizeDate(req.repeatType(), req.date()))
                .timezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim())
                .reminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore())
                .enabled(req.enabled() == null ? true : req.enabled())
                .build();

        return repo.save(s);
    }

    public PracticeSchedule updateForUser(Long userId, Long scheduleId, PracticeScheduleRequest req) {
        validate(req);
        enforceHasTrainer(userId);

        PracticeSchedule s = repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        s.setTitle(req.title().trim());
        s.setStartTime(req.startTime());
        s.setRepeatType(req.repeatType());
        s.setDaysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()));
        s.setDate(normalizeDate(req.repeatType(), req.date()));
        s.setTimezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim());
        s.setReminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore());
        s.setEnabled(req.enabled() == null ? s.isEnabled() : req.enabled());

        return repo.save(s);
    }

    public void deleteForUser(Long userId, Long scheduleId) {
        repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        repo.deleteByIdAndUserId(scheduleId, userId);
    }

    public PracticeSchedule getForUser(Long userId, Long scheduleId) {
        return repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    }

    public List<PracticeSchedule> listForUser(Long userId) {
        return repo.findAllByUserIdOrderByStartTimeAsc(userId);
    }

    private void enforceHasTrainer(Long userId) {
        boolean hasTrainer = userTrainerRepo.existsByUserId(userId);
        if (!hasTrainer) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be assigned to a trainer");
        }
    }

    private void validate(PracticeScheduleRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is null");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (req.startTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        if (req.repeatType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repeat type is required");
        }

        // DTO već ima @AssertTrue, ali ovdje dodajemo service-side guard da ne uđe invalid stanje u bazu.
        switch (req.repeatType()) {
            case DAILY -> {
                if (req.daysOfWeek() != null && !req.daysOfWeek().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "daysOfWeek must be empty for DAILY");
                }
                if (req.date() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be null for DAILY");
                }
            }
            case WEEKLY -> {
                if (req.daysOfWeek() == null || req.daysOfWeek().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "daysOfWeek is required for WEEKLY");
                }
                if (req.date() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be null for WEEKLY");
                }
            }
            case ONCE -> {
                if (req.date() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required for ONCE");
                }
                if (req.daysOfWeek() != null && !req.daysOfWeek().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "daysOfWeek must be empty for ONCE");
                }
            }
        }
    }

    private Set<DayOfWeek> normalizeDays(RepeatType type, Set<DayOfWeek> days) {
        if (type == RepeatType.DAILY || type == RepeatType.ONCE) {
            return new HashSet<>(); // ignoriramo
        }
        return days == null ? new HashSet<>() : new HashSet<>(days);
    }

    private LocalDate normalizeDate(RepeatType type, LocalDate date) {
        return type == RepeatType.ONCE ? date : null;
    }
}
