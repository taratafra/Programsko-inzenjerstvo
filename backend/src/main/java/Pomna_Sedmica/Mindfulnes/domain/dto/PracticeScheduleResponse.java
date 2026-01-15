package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record PracticeScheduleResponse(
        Long id,
        Long userId,
        String title,
        LocalTime startTime,
        RepeatType repeatType,
        Set<DayOfWeek> daysOfWeek,
        LocalDate date,
        String timezone,
        Integer reminderMinutesBefore,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Set<LocalDate> excludedDates,
        Long trainerId
) {
    public static PracticeScheduleResponse from(PracticeSchedule s) {
        return new PracticeScheduleResponse(
                s.getId(),
                s.getUserId(),
                s.getTitle(),
                s.getStartTime(),
                s.getRepeatType(),
                s.getDaysOfWeek(),
                s.getDate(),
                s.getTimezone(),
                s.getReminderMinutesBefore(),
                s.isEnabled(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getExcludedDates(),
                s.getTrainerId()
        );
    }
}
