package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcsExportService {

    private final PracticeScheduleRepository schedules;

    public String buildIcsForUser(Long userId) {
        List<PracticeSchedule> list = schedules.findAllByUserIdOrderByStartTimeAsc(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Pomna Sedmica//Mindfulnes//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");

        for (PracticeSchedule s : list) {
            sb.append(buildEventForSchedule(s));
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private String buildEventForSchedule(PracticeSchedule s) {
        // Napomena: ICS “event” s RRULE predstavlja ponavljajući event.
        // DTSTART bez konkretnog datuma nije idealan, ali za lab i import radi ok
        // ako user u kalendaru “adjusta” prvi datum. Ako želite, možemo kasnije
        // postaviti DTSTART na “najbliži idući termin” kao u ReminderEngine-u.

        String uid = UUID.randomUUID() + "@mindfulnes";
        String tz = (s.getTimezone() == null || s.getTimezone().isBlank())
                ? PracticeScheduleService.DEFAULT_TZ
                : s.getTimezone().trim();

        String time = s.getStartTime().toString().replace(":", "") + "00"; // HHmmss
        // Placeholder DTSTART (današnji datum ne znamo bez “now”, ostavimo 19700101)
        String dtstart = "19700101T" + time;

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("SUMMARY:").append(escape(s.getTitle())).append("\r\n");
        sb.append("DTSTART;TZID=").append(tz).append(":").append(dtstart).append("\r\n");
        sb.append("RRULE:").append(rrule(s)).append("\r\n");
        sb.append("END:VEVENT\r\n");

        return sb.toString();
    }

    private String rrule(PracticeSchedule s) {
        if (s.getRepeatType() == RepeatType.DAILY) {
            return "FREQ=DAILY";
        }
        Set<DayOfWeek> days = s.getDaysOfWeek();
        String byday = (days == null ? Set.<DayOfWeek>of() : days).stream()
                .sorted(Comparator.naturalOrder())
                .map(this::icsDay)
                .collect(Collectors.joining(","));
        return "FREQ=WEEKLY;BYDAY=" + byday;
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
}
