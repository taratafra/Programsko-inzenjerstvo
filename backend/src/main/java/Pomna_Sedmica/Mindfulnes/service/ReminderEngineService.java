package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.ReminderLog;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderStatus;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.ReminderLogRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule; // <-- provjeri package/ime
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository; // <-- provjeri package/ime
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderEngineService {

    private final PracticeScheduleRepository schedules;
    private final ReminderLogRepository reminderLogs;
    private final UserRepository users;
    private final List<ReminderSender> senders; // Email + Push stubovi

    /**
     * Poll prozor: sada -> sada+lookAheadSeconds
     * Ako reminder upadne unutra i nije već poslan, šaljemo.
     */
    @Transactional
    public int pollAndSend(int lookAheadSeconds) {
        Instant now = Instant.now();
        Instant until = now.plusSeconds(lookAheadSeconds);

        // Učitaj sve enabled scheduleove (ako nemaš ovu metodu, napravi findByEnabledTrue)
        List<PracticeSchedule> enabled = schedules.findByEnabledTrue();

        int sentCount = 0;

        for (PracticeSchedule s : enabled) {
            if (s.getReminderMinutesBefore() == null) continue; // ako nema podsjetnika, preskoči

            ZoneId zone = safeZone(s.getTimezone());
            ZonedDateTime nextStart = computeNextStart(s, zone, ZonedDateTime.now(zone));

            if (nextStart == null) continue;

            Instant dueAt = nextStart.minusMinutes(s.getReminderMinutesBefore()).toInstant();

            // je li dueAt u prozoru?
            if (dueAt.isBefore(now) || !dueAt.isBefore(until)) {
                continue;
            }

            // pošalji na sve kanale koje želiš (za sad oba)
            User user = users.findById(s.getUserId()).orElse(null);
            if (user == null) continue;

            for (ReminderSender sender : senders) {
                ReminderChannel channel = sender.channel();

                // anti-duplicate
                if (reminderLogs.existsByScheduleIdAndDueAtAndChannel(s.getId(), dueAt, channel)) {
                    continue;
                }

                try {
                    String text = buildReminderText(s, nextStart, zone);
                    sender.send(user, text);

                    reminderLogs.save(ReminderLog.builder()
                            .userId(s.getUserId())
                            .scheduleId(s.getId())
                            .dueAt(dueAt)
                            .sentAt(Instant.now())
                            .channel(channel)
                            .status(ReminderStatus.SENT)
                            .errorMessage(null)
                            .build());

                    sentCount++;
                } catch (Exception ex) {
                    reminderLogs.save(ReminderLog.builder()
                            .userId(s.getUserId())
                            .scheduleId(s.getId())
                            .dueAt(dueAt)
                            .sentAt(null)
                            .channel(channel)
                            .status(ReminderStatus.FAILED)
                            .errorMessage(ex.getMessage())
                            .build());
                }
            }
        }

        return sentCount;
    }

    private ZoneId safeZone(String tz) {
        try {
            return (tz == null || tz.isBlank()) ? ZoneId.of("Europe/Zagreb") : ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.of("Europe/Zagreb");
        }
    }

    private String buildReminderText(PracticeSchedule s, ZonedDateTime nextStart, ZoneId zone) {
        String time = nextStart.toLocalTime().toString(); // "08:00"
        String title = (s.getTitle() == null || s.getTitle().isBlank()) ? "Practice" : s.getTitle();
        return "Reminder: \"" + title + "\" starts at " + time + " (" + zone + ").";
    }

    /**
     * Račun sljedećeg starta:
     * DAILY: danas u startTime ako još nije prošlo, inače sutra
     * WEEKLY: sljedeći odabrani dan u daysOfWeek (uključujući danas ako još nije prošlo)
     */
    private ZonedDateTime computeNextStart(PracticeSchedule s, ZoneId zone, ZonedDateTime now) {
        LocalTime start = s.getStartTime();
        if (start == null) return null;

        if (s.getRepeatType() == RepeatType.DAILY) {
            ZonedDateTime candidate = now.with(start);
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1).with(start);
            }
            return candidate;
        }

        if (s.getRepeatType() == RepeatType.WEEKLY) {
            Set<DayOfWeek> days = s.getDaysOfWeek();
            if (days == null || days.isEmpty()) return null;

            // provjeri danas
            ZonedDateTime today = now.with(start);
            if (days.contains(now.getDayOfWeek()) && today.isAfter(now)) {
                return today;
            }

            // nađi sljedeći dan
            for (int i = 1; i <= 7; i++) {
                ZonedDateTime d = now.plusDays(i).with(start);
                if (days.contains(d.getDayOfWeek())) {
                    return d;
                }
            }
        }

        return null;
    }
}
