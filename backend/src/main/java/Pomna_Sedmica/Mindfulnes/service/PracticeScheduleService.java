package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.PracticeScheduleRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserTrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeScheduleService {

    public static final String DEFAULT_TZ = "Europe/Zagreb";

    private final PracticeScheduleRepository repo;
    private final UserTrainerRepository userTrainerRepo;

    public PracticeSchedule createForUser(Long userId, PracticeScheduleRequest req) {
        validate(req);
        enforceHasTrainer(userId);

        boolean isSubscribed = userTrainerRepo.findByUserIdAndTrainerId(userId, req.trainerId()).isPresent();
        if (!isSubscribed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not subscribed to this trainer");
        }

        // Convert excluded dates from String to LocalDate
        Set<LocalDate> convertedExcludedDates = convertExcludedDates(req.excludedDates());

        PracticeSchedule s = PracticeSchedule.builder()
                .userId(userId)
                .trainerId(req.trainerId())
                .title(req.title().trim())
                .startTime(req.startTime())
                .repeatType(req.repeatType())
                .daysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()))
                .date(normalizeDate(req.repeatType(), req.date()))
                .timezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim())
                .reminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore())
                .enabled(req.enabled() == null ? true : req.enabled())
                .excludedDates(convertedExcludedDates)
                .build();

        return repo.save(s);
    }

    public PracticeSchedule updateForUser(Long userId, Long scheduleId, PracticeScheduleRequest req) {
        validate(req);
        enforceHasTrainer(userId);

        boolean isSubscribed = userTrainerRepo.findByUserIdAndTrainerId(userId, req.trainerId()).isPresent();
        if (!isSubscribed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not subscribed to this trainer");
        }

        PracticeSchedule s = repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        // Convert excluded dates from String to LocalDate
        Set<LocalDate> convertedExcludedDates = convertExcludedDates(req.excludedDates());

        s.setTrainerId(req.trainerId());
        s.setTitle(req.title().trim());
        s.setStartTime(req.startTime());
        s.setRepeatType(req.repeatType());
        s.setDaysOfWeek(normalizeDays(req.repeatType(), req.daysOfWeek()));
        s.setDate(normalizeDate(req.repeatType(), req.date()));
        s.setTimezone((req.timezone() == null || req.timezone().isBlank()) ? DEFAULT_TZ : req.timezone().trim());
        s.setReminderMinutesBefore(req.reminderMinutesBefore() == null ? 10 : req.reminderMinutesBefore());
        s.setEnabled(req.enabled() == null ? s.isEnabled() : req.enabled());
        s.setExcludedDates(convertedExcludedDates);

        return repo.save(s);
    }

    @Transactional
    public void deleteForUser(Long userId, Long scheduleId) {
        PracticeSchedule schedule = repo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        repo.delete(schedule);
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
            return new HashSet<>();
        }
        return days == null ? new HashSet<>() : new HashSet<>(days);
    }

    private LocalDate normalizeDate(RepeatType type, LocalDate date) {
        return type == RepeatType.ONCE ? date : null;
    }

    /**
     * Converts a set of date strings (YYYY-MM-DD) to LocalDate objects.
     * Handles both String and LocalDate inputs for flexibility.
     */
    private Set<LocalDate> convertExcludedDates(Set<?> excludedDates) {
        if (excludedDates == null || excludedDates.isEmpty()) {
            return new HashSet<>();
        }

        return excludedDates.stream()
                .map(dateObj -> {
                    if (dateObj instanceof LocalDate) {
                        // Already a LocalDate
                        return (LocalDate) dateObj;
                    } else if (dateObj instanceof String) {
                        // Parse string to LocalDate
                        return LocalDate.parse((String) dateObj);
                    } else {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid excluded date format: " + dateObj
                        );
                    }
                })
                .collect(Collectors.toSet());
    }

    public List<PracticeSchedule> listForTrainer(Long trainerId) {
        return repo.findAllByTrainerIdOrderByStartTimeAsc(trainerId);
    }
}