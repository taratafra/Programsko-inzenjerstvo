package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcsExportService {

    private final PracticeScheduleRepository schedules;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC_STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    // RFC5545 line folding recommendation
    private static final int ICS_FOLD_LEN = 75;

    public String buildIcsForUser(Long userId) {
        List<PracticeSchedule> list = schedules.findAllByUserIdOrderByStartTimeAsc(userId);

        // Collect tzids that appear in events -> emit VTIMEZONE components for compatibility (Outlook/Apple)
        Set<String> tzids = list.stream()
                .map(this::resolveTzid)
                .filter(tz -> tz != null && !tz.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        StringBuilder sb = new StringBuilder();
        appendFoldedLine(sb, "BEGIN:VCALENDAR");
        appendFoldedLine(sb, "VERSION:2.0");
        appendFoldedLine(sb, "PRODID:-//Pomna Sedmica//Mindfulness Practice//EN");
        appendFoldedLine(sb, "CALSCALE:GREGORIAN");
        appendFoldedLine(sb, "METHOD:PUBLISH");

        // Apple/Google metadata
        appendFoldedLine(sb, "X-WR-CALNAME:Mindfulness Practice");
        appendFoldedLine(sb, "X-WR-TIMEZONE:" + PracticeScheduleService.DEFAULT_TZ);

        for (String tzid : tzids) {
            sb.append(buildVTimeZoneComponent(tzid));
        }

        for (PracticeSchedule s : list) {
            if (s != null && s.isEnabled()) {
                sb.append(buildEventForSchedule(s));
            }
        }

        appendFoldedLine(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private String buildEventForSchedule(PracticeSchedule s) {
        // Stable UID
        String uid = "schedule-" + s.getId() + "@mindfulness.app";

        String tz = resolveTzid(s);
        ZoneId zone = ZoneId.of(tz);

        LocalTime startTime = s.getStartTime();

        LocalDate startDate;
        if (s.getRepeatType() == RepeatType.ONCE && s.getDate() != null) {
            startDate = s.getDate();
        } else {
            startDate = calculateNextOccurrence(s, zone);
        }

        ZonedDateTime start = ZonedDateTime.of(startDate, startTime, zone);
        ZonedDateTime end = start.plusHours(1);

        String dtstart = start.format(DATE_TIME_FMT);
        String dtend = end.format(DATE_TIME_FMT);

        // DTSTAMP must be UTC "now"
        String dtstamp = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_STAMP_FMT);

        // Use entity timestamps for CREATED/LAST-MODIFIED
        // Convert Instant -> UTC stamp
        String created = (s.getCreatedAt() != null)
                ? ZonedDateTime.ofInstant(s.getCreatedAt(), ZoneOffset.UTC).format(UTC_STAMP_FMT)
                : dtstamp;

        String lastModified = (s.getUpdatedAt() != null)
                ? ZonedDateTime.ofInstant(s.getUpdatedAt(), ZoneOffset.UTC).format(UTC_STAMP_FMT)
                : dtstamp;

        // SEQUENCE should increase when event changes.
        // Easiest deterministic approach without extra DB column:
        // derive from updatedAt epoch seconds (stable & changes when updatedAt changes)
        int sequence = 0;
        if (s.getUpdatedAt() != null) {
            long secs = s.getUpdatedAt().getEpochSecond();
            sequence = (int) (secs % Integer.MAX_VALUE);
        }

        StringBuilder sb = new StringBuilder();
        appendFoldedLine(sb, "BEGIN:VEVENT");
        appendFoldedLine(sb, "UID:" + uid);
        appendFoldedLine(sb, "DTSTAMP:" + dtstamp);
        appendFoldedLine(sb, "CREATED:" + created);
        appendFoldedLine(sb, "LAST-MODIFIED:" + lastModified);
        appendFoldedLine(sb, "SEQUENCE:" + sequence);

        // TZID local time with matching VTIMEZONE improves Outlook/Apple compatibility
        appendFoldedLine(sb, "DTSTART;TZID=" + tz + ":" + dtstart);
        appendFoldedLine(sb, "DTEND;TZID=" + tz + ":" + dtend);

        appendFoldedLine(sb, "SUMMARY:" + escape(s.getTitle()));

        String description = buildDescription(s);
        if (!description.isEmpty()) {
            appendFoldedLine(sb, "DESCRIPTION:" + escape(description));
        }

        // Recurrence
        if (s.getRepeatType() != RepeatType.ONCE) {
            String rrule = buildRRule(s);
            if (rrule != null && !rrule.isBlank()) {
                appendFoldedLine(sb, "RRULE:" + rrule);
            }

            Set<LocalDate> excludedDates = s.getExcludedDates();
            if (excludedDates != null && !excludedDates.isEmpty()) {
                String exdate = excludedDates.stream()
                        .sorted()
                        .map(date -> ZonedDateTime.of(date, startTime, zone).format(DATE_TIME_FMT))
                        .collect(Collectors.joining(","));
                appendFoldedLine(sb, "EXDATE;TZID=" + tz + ":" + exdate);
            }
        }

        // Alarm
        if (s.getReminderMinutesBefore() != null && s.getReminderMinutesBefore() > 0) {
            appendFoldedLine(sb, "BEGIN:VALARM");
            appendFoldedLine(sb, "TRIGGER:-PT" + s.getReminderMinutesBefore() + "M");
            appendFoldedLine(sb, "ACTION:DISPLAY");
            appendFoldedLine(sb, "DESCRIPTION:" + escape(s.getTitle()) + " starts soon");
            appendFoldedLine(sb, "END:VALARM");
        }

        appendFoldedLine(sb, "END:VEVENT");
        return sb.toString();
    }

    private String resolveTzid(PracticeSchedule s) {
        String tz = (s.getTimezone() == null || s.getTimezone().isBlank())
                ? PracticeScheduleService.DEFAULT_TZ
                : s.getTimezone().trim();
        return tz;
    }

    private LocalDate calculateNextOccurrence(PracticeSchedule s, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);

        if (s.getRepeatType() == RepeatType.DAILY) {
            return today;
        }

        if (s.getRepeatType() == RepeatType.WEEKLY) {
            Set<DayOfWeek> days = s.getDaysOfWeek();
            if (days == null || days.isEmpty()) return today;

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

    /**
     * Build VTIMEZONE using RDATE entries for a practical year range.
     * This is widely accepted by Outlook/Apple and avoids tricky RRULE nuances.
     */
    private String buildVTimeZoneComponent(String tzid) {
        ZoneId zoneId = ZoneId.of(tzid);
        ZoneRules rules = zoneId.getRules();
        List<ZoneOffsetTransitionRule> transitionRules = rules.getTransitionRules();

        StringBuilder sb = new StringBuilder();
        appendFoldedLine(sb, "BEGIN:VTIMEZONE");
        appendFoldedLine(sb, "TZID:" + tzid);
        appendFoldedLine(sb, "X-LIC-LOCATION:" + tzid);

        int yearStart = Year.now().getValue() - 1;
        int yearEnd = Year.now().getValue() + 10;

        if (transitionRules == null || transitionRules.isEmpty()) {
            ZoneOffset off = rules.getOffset(Instant.now());
            appendFoldedLine(sb, "BEGIN:STANDARD");
            appendFoldedLine(sb, "DTSTART:19700101T000000");
            appendFoldedLine(sb, "TZOFFSETFROM:" + formatOffset(off));
            appendFoldedLine(sb, "TZOFFSETTO:" + formatOffset(off));
            appendFoldedLine(sb, "TZNAME:" + off.getId());
            appendFoldedLine(sb, "END:STANDARD");
            appendFoldedLine(sb, "END:VTIMEZONE");
            return sb.toString();
        }

        List<ZoneOffsetTransitionRule> daylightRules = new ArrayList<>();
        List<ZoneOffsetTransitionRule> standardRules = new ArrayList<>();

        for (ZoneOffsetTransitionRule r : transitionRules) {
            if (r.getOffsetAfter().getTotalSeconds() > r.getOffsetBefore().getTotalSeconds()) {
                daylightRules.add(r);
            } else {
                standardRules.add(r);
            }
        }

        if (!standardRules.isEmpty()) {
            sb.append(buildTzSubComponent("STANDARD", standardRules, yearStart, yearEnd));
        }
        if (!daylightRules.isEmpty()) {
            sb.append(buildTzSubComponent("DAYLIGHT", daylightRules, yearStart, yearEnd));
        }

        appendFoldedLine(sb, "END:VTIMEZONE");
        return sb.toString();
    }

    private String buildTzSubComponent(String type,
                                       List<ZoneOffsetTransitionRule> rules,
                                       int yearStart,
                                       int yearEnd) {
        ZoneOffsetTransitionRule first = rules.get(0);

        StringBuilder sb = new StringBuilder();
        appendFoldedLine(sb, "BEGIN:" + type);

        ZoneOffsetTransition firstTr = first.createTransition(yearStart);
        appendFoldedLine(sb, "DTSTART:" + firstTr.getDateTimeBefore().format(DATE_TIME_FMT));

        appendFoldedLine(sb, "TZOFFSETFROM:" + formatOffset(first.getOffsetBefore()));
        appendFoldedLine(sb, "TZOFFSETTO:" + formatOffset(first.getOffsetAfter()));
        appendFoldedLine(sb, "TZNAME:" + (type.equals("DAYLIGHT") ? "DST" : "STD"));

        List<String> rdates = new ArrayList<>();
        for (ZoneOffsetTransitionRule r : rules) {
            for (int y = yearStart; y <= yearEnd; y++) {
                ZoneOffsetTransition tr = r.createTransition(y);
                rdates.add(tr.getDateTimeBefore().format(DATE_TIME_FMT));
            }
        }
        rdates.sort(String::compareTo);

        appendFoldedLine(sb, "RDATE:" + String.join(",", rdates));

        appendFoldedLine(sb, "END:" + type);
        return sb.toString();
    }

    private String formatOffset(ZoneOffset offset) {
        int totalSeconds = offset.getTotalSeconds();
        int abs = Math.abs(totalSeconds);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        return String.format("%s%02d%02d", totalSeconds >= 0 ? "+" : "-", hours, minutes);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\r", "")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /**
     * Append ICS line with CRLF and RFC5545 folding (75 chars).
     */
    private void appendFoldedLine(StringBuilder sb, String line) {
        if (line == null) line = "";

        if (line.length() <= ICS_FOLD_LEN) {
            sb.append(line).append("\r\n");
            return;
        }

        sb.append(line, 0, Math.min(ICS_FOLD_LEN, line.length())).append("\r\n");
        int idx = ICS_FOLD_LEN;

        while (idx < line.length()) {
            sb.append(' ');
            int next = Math.min(idx + (ICS_FOLD_LEN - 1), line.length());
            sb.append(line, idx, next).append("\r\n");
            idx = next;
        }
    }
}