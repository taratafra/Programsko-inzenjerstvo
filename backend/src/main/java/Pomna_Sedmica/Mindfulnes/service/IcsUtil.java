package Pomna_Sedmica.Mindfulnes.service;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class IcsUtil {

    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private IcsUtil() {}

    public static String nowUtc() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FMT);
    }

    /**
     * Problem: schedule ima samo LocalTime (nema LocalDate).
     * ICS treba DATETIME → uzmemo današnji datum u toj zoni (dovoljno dobro za “recurring”).
     */
    public static String formatLocalDateTimeInTz(String tz, LocalTime startTime) {
        ZoneId zone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zone);
        LocalDateTime ldt = LocalDateTime.of(today, startTime);
        return ldt.format(LOCAL_FMT);
    }

    public static String toIcsDay(DayOfWeek d) {
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

    public static String escapeText(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}
