package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcsExportService {

    private final PracticeScheduleRepository schedules;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String buildIcsForUser(Long userId) {
        List<PracticeSchedule> list = schedules.findAllByUserIdOrderByStartTimeAsc(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Pomna Sedmica//Mindfulness Practice//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");

        for (PracticeSchedule s : list) {
            sb.append(buildEventForSchedule(s));
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private String buildEventForSchedule(PracticeSchedule s) {
        String uid = UUID.randomUUID() + "@mindfulnes.app";
        String tz = (s.getTimezone() == null || s.getTimezone().isBlank())
                ? PracticeScheduleService.DEFAULT_TZ
                : s.getTimezone().trim();

        ZoneId zone = ZoneId.of(tz);
        LocalTime startTime = s.getStartTime();

        // Calculate a reasonable start date
        LocalDate startDate;
        if (s.getRepeatType() == RepeatType.ONCE && s.getDate() != null) {
            // For ONCE events, use the specific date
            startDate = s.getDate();
        } else {
            // For recurring events, start from today or the next occurrence
            startDate = calculateNextOccurrence(s, zone);
        }

        // Create start datetime in the schedule's timezone
        ZonedDateTime start = ZonedDateTime.of(startDate, startTime, zone);

        // Default duration: 1 hour (you can add a duration field to PracticeSchedule if needed)
        ZonedDateTime end = start.plusHours(1);

        // Format as local time (not UTC) with timezone reference
        String dtstart = start.format(DATE_TIME_FMT);
        String dtend = end.format(DATE_TIME_FMT);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(formatNowUtc()).append("\r\n");
        sb.append("DTSTART;TZID=").append(tz).append(":").append(dtstart).append("\r\n");
        sb.append("DTEND;TZID=").append(tz).append(":").append(dtend).append("\r\n");
        sb.append("SUMMARY:").append(escape(s.getTitle())).append("\r\n");

        // Add description with schedule details
        String description = buildDescription(s);
        if (!description.isEmpty()) {
            sb.append("DESCRIPTION:").append(escape(description)).append("\r\n");
        }

        // Add recurrence rule for recurring events
        if (s.getRepeatType() != RepeatType.ONCE) {
            sb.append("RRULE:").append(buildRRule(s)).append("\r\n");

            // Add excluded dates if any
            Set<LocalDate> excludedDates = s.getExcludedDates();
            if (excludedDates != null && !excludedDates.isEmpty()) {
                sb.append("EXDATE;TZID=").append(tz).append(":");
                sb.append(excludedDates.stream()
                        .sorted() // Sort for consistency
                        .map(date -> {
                            ZonedDateTime excludedDateTime = ZonedDateTime.of(date, startTime, zone);
                            return excludedDateTime.format(DATE_TIME_FMT);
                        })
                        .collect(Collectors.joining(",")));
                sb.append("\r\n");
            }
        }

        // Add reminder/alarm if configured
        if (s.getReminderMinutesBefore() != null && s.getReminderMinutesBefore() > 0) {
            sb.append("BEGIN:VALARM\r\n");
            sb.append("TRIGGER:-PT").append(s.getReminderMinutesBefore()).append("M\r\n");
            sb.append("ACTION:DISPLAY\r\n");
            sb.append("DESCRIPTION:").append(escape(s.getTitle())).append(" starts soon\r\n");
            sb.append("END:VALARM\r\n");
        }

        sb.append("END:VEVENT\r\n");

        return sb.toString();
    }

    private LocalDate calculateNextOccurrence(PracticeSchedule s, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);

        if (s.getRepeatType() == RepeatType.DAILY) {
            // Start from today
            return today;
        }

        if (s.getRepeatType() == RepeatType.WEEKLY) {
            Set<DayOfWeek> days = s.getDaysOfWeek();
            if (days == null || days.isEmpty()) {
                return today;
            }

            // Find the next day of week
            for (int i = 0; i < 7; i++) {
                LocalDate candidate = today.plusDays(i);
                if (days.contains(candidate.getDayOfWeek())) {
                    return candidate;
                }
            }
        }

        return today;
    }

    private String buildDescription(PracticeSchedule s) {
        StringBuilder desc = new StringBuilder();
        desc.append("Mindfulness Practice Session\\n\\n");

        if (s.getRepeatType() == RepeatType.DAILY) {
            desc.append("Repeats: Daily\\n");
        } else if (s.getRepeatType() == RepeatType.WEEKLY && s.getDaysOfWeek() != null) {
            String daysStr = s.getDaysOfWeek().stream()
                    .sorted()
                    .map(d -> d.toString().substring(0, 3))
                    .collect(Collectors.joining(", "));
            desc.append("Repeats: Weekly on ").append(daysStr).append("\\n");
        }

        if (s.getReminderMinutesBefore() != null) {
            desc.append("Reminder: ").append(s.getReminderMinutesBefore()).append(" minutes before");
        }

        return desc.toString();
    }

    private String buildRRule(PracticeSchedule s) {
        if (s.getRepeatType() == RepeatType.DAILY) {
            return "FREQ=DAILY";
        }

        if (s.getRepeatType() == RepeatType.WEEKLY) {
            Set<DayOfWeek> days = s.getDaysOfWeek();
            if (days == null || days.isEmpty()) {
                return "FREQ=WEEKLY";
            }

            String byday = days.stream()
                    .sorted(Comparator.naturalOrder())
                    .map(this::icsDay)
                    .collect(Collectors.joining(","));
            return "FREQ=WEEKLY;BYDAY=" + byday;
        }

        // For ONCE events, no RRULE needed
        return "";
    }

    private String icsDay(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private String formatNowUtc() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }
}