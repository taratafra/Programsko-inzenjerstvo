package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record PracticeScheduleRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "repeatType is required")
        RepeatType repeatType,

        // Za DAILY moze biti null ili prazno
        Set<DayOfWeek> daysOfWeek,

        // Optional, default Europe/Zagreb
        String timezone,

        @Min(value = 0, message = "reminderMinutesBefore must be >= 0")
        @Max(value = 1440, message = "reminderMinutesBefore must be <= 1440")
        Integer reminderMinutesBefore,

        // Optional; default true
        Boolean enabled,

        /**
         * Optional:
         * - ako dođe -> koristi ga (override)
         * - ako ne dođe -> uzmi primary trainer iz user_trainer
         * - ako nema ni primary -> 400 (ne može schedule bez trenera)
         */
        Long trainerId
) {}
