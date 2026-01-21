package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Request DTO za stvaranje/azuriranje termina.
 *
 * - DAILY  : startTime obavezno, daysOfWeek/date ignoriraj
 * - WEEKLY : startTime + daysOfWeek obavezno, date must be null
 * - ONCE   : startTime + date obavezno, daysOfWeek ignoriraj
 */
public record PracticeScheduleRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "repeatType is required")
        RepeatType repeatType,

        // WEEKLY: required (non-empty). DAILY/ONCE: treba biti null/empty.
        Set<DayOfWeek> daysOfWeek,

        // ONCE: obavezno. DAILY/WEEKLY: treba biti null.
        LocalDate date,

        // Optional, default Europe/Zagreb
        String timezone,

        @Min(value = 0, message = "reminderMinutesBefore must be >= 0")
        @Max(value = 1440, message = "reminderMinutesBefore must be <= 1440")
        Integer reminderMinutesBefore,

        // Optional; default true
        Boolean enabled,

        @NotNull(message = "trainerId is required")
        Long trainerId,

        Set<String> excludedDates
) {
    @AssertTrue(message = "Invalid combination: WEEKLY requires non-empty daysOfWeek; ONCE requires date; DAILY should not set daysOfWeek/date")
    public boolean isValidCombination() {
        if (repeatType == null) return true; // other validators will catch null

        return switch (repeatType) {
            case DAILY -> (daysOfWeek == null || daysOfWeek.isEmpty()) && date == null;
            case WEEKLY -> (daysOfWeek != null && !daysOfWeek.isEmpty()) && date == null;
            case ONCE -> date != null; // daysOfWeek can be null/empty, doesn't matter
        };
    }
}