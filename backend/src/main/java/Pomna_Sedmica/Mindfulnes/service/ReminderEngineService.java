package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.entity.ReminderLog;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderStatus;
import Pomna_Sedmica.Mindfulnes.domain.enums.RepeatType;
import Pomna_Sedmica.Mindfulnes.repository.PracticeScheduleRepository;
import Pomna_Sedmica.Mindfulnes.repository.ReminderLogRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReminderEngineService {

    private final PracticeScheduleRepository schedules;
    private final UserRepository users;
    private final ReminderLogRepository logs;

    private final EmailReminderSenderStub emailSender;
    private final PushReminderSenderStub pushSender;

    // ✅ Look-ahead buffer to catch reminders before their exact time
    private static final long REMINDER_LOOKAHEAD_SECONDS = 30;

    // ✅ Time window to prevent duplicate reminders (5 minutes)
    private static final long REMINDER_WINDOW_SECONDS = 300; // 5 minutes

    /**
     * Glavni entrypoint koji se može zvati iz schedulera (npr. @Scheduled).
     * Ide kroz enabled schedule-ove i šalje reminder(e) ako treba.
     */
    @Transactional
    public synchronized void tick() {
        Instant now = Instant.now();

        List<PracticeSchedule> enabled = schedules.findByEnabledTrue();
        for (PracticeSchedule s : enabled) {
            Optional<User> uOpt = users.findById(s.getUserId());
            if (uOpt.isEmpty()) continue;

            User u = uOpt.get();

            // 1) izračunaj sljedeći start termina (u schedule tz)
            Optional<Instant> occOpt = computeNextOccurrenceStart(s, now);
            if (occOpt.isEmpty()) continue;

            Instant occurrenceStartAt = occOpt.get();

            // 2) reminder time = start - minutesBefore
            Integer minutesBeforeObj = s.getReminderMinutesBefore();
            int minutesBefore = minutesBeforeObj == null ? 10 : minutesBeforeObj;
            Instant reminderAt = occurrenceStartAt.minusSeconds(minutesBefore * 60L);

            // ✅ 3) Only send if we're within the reminder window
            // Window: [reminderAt - 30s, reminderAt + 5 minutes]
            Instant windowStart = reminderAt.minusSeconds(REMINDER_LOOKAHEAD_SECONDS);
            Instant windowEnd = reminderAt.plusSeconds(REMINDER_WINDOW_SECONDS);

            if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
                continue;
            }

            // ✅ 4) Send both EMAIL and PUSH notifications
            boolean emailSent = sendOnce(s, u, ReminderChannel.EMAIL, reminderAt, occurrenceStartAt, minutesBefore);
            boolean pushSent = sendOnce(s, u, ReminderChannel.PUSH, reminderAt, occurrenceStartAt, minutesBefore);

            // ✅ 5) Auto-disable ONCE schedules after both channels attempted
            if ((emailSent || pushSent) && s.getRepeatType() == RepeatType.ONCE) {
                s.setEnabled(false);
                schedules.save(s);
            }
        }
    }

    @Transactional
    protected boolean sendOnce(PracticeSchedule s,
                               User u,
                               ReminderChannel channel,
                               Instant reminderAt,
                               Instant occurrenceStartAt,
                               int minutesBefore) {

        // ✅ Single robust check using custom query
        if (logs.hasReminderBeenSent(s.getId(), channel, occurrenceStartAt)) {
            return false;
        }

        ReminderLog log = ReminderLog.builder()
                .userId(u.getId())
                .scheduleId(s.getId())
                .channel(channel)
                .status(ReminderStatus.PENDING)
                .reminderAt(reminderAt)
                .occurrenceStartAt(occurrenceStartAt)
                .sentAt(null)
                .build();

        log = logs.save(log);

        try {
            String messageText = buildReminderText(s, occurrenceStartAt, minutesBefore);

            if (channel == ReminderChannel.EMAIL) {
                emailSender.send(u, messageText);
            } else {
                pushSender.send(u, messageText);
            }

            log.setStatus(ReminderStatus.SENT);
            log.setSentAt(Instant.now());
            log.setErrorMessage(null);
            logs.save(log);

            return true;

        } catch (Exception ex) {
            log.setStatus(ReminderStatus.FAILED);
            log.setSentAt(Instant.now());
            log.setErrorMessage(ex.getMessage());
            logs.save(log);
            return false;
        }
    }

    private String buildReminderText(PracticeSchedule s, Instant occurrenceStartAt, int minutesBefore) {
        return "🔔 Reminder: \"" + s.getTitle() + "\" starts in " + minutesBefore
                + " minutes!";
    }

    /**
     * Izračun "sljedećeg starta" termina u vremenskoj zoni schedule-a.
     * ONCE: s.date u startTime ako je u budućnosti.
     * DAILY: danas u startTime ako nije prošlo; inače sutra.
     * WEEKLY: najbliži iduće od daysOfWeek (u max 7 dana).
     */
    private Optional<Instant> computeNextOccurrenceStart(PracticeSchedule s, Instant nowUtc) {
        String tz = (s.getTimezone() == null || s.getTimezone().isBlank())
                ? PracticeScheduleService.DEFAULT_TZ
                : s.getTimezone().trim();

        ZoneId zone;
        try {
            zone = ZoneId.of(tz);
        } catch (Exception e) {
            zone = ZoneId.of(PracticeScheduleService.DEFAULT_TZ);
        }

        ZonedDateTime now = nowUtc.atZone(zone);

        LocalTime startTime = s.getStartTime();
        if (startTime == null) return Optional.empty();

        // ONCE: exactly on s.date at startTime (schedule timezone)
        if (s.getRepeatType() == RepeatType.ONCE) {
            LocalDate date = s.getDate();
            if (date == null) return Optional.empty();

            ZonedDateTime onceAt = date.atTime(startTime).atZone(zone);
            return onceAt.isAfter(now) ? Optional.of(onceAt.toInstant()) : Optional.empty();
        }

        if (s.getRepeatType() == RepeatType.DAILY) {
            ZonedDateTime today = now.toLocalDate().atTime(startTime).atZone(zone);
            ZonedDateTime next = today.isAfter(now) ? today : today.plusDays(1);
            return Optional.of(next.toInstant());
        }

        // WEEKLY
        Set<DayOfWeek> days = s.getDaysOfWeek();
        if (days == null || days.isEmpty()) return Optional.empty();

        ZonedDateTime best = null;
        for (int i = 0; i <= 7; i++) {
            LocalDate d = now.toLocalDate().plusDays(i);
            if (!days.contains(d.getDayOfWeek())) continue;

            ZonedDateTime cand = d.atTime(startTime).atZone(zone);
            if (!cand.isAfter(now)) continue;

            if (best == null || cand.isBefore(best)) best = cand;
        }

        if (best == null) {
            DayOfWeek minDay = days.stream().min(Comparator.naturalOrder()).orElse(null);
            if (minDay == null) return Optional.empty();
            LocalDate nextWeek = now.toLocalDate().with(TemporalAdjusters.next(minDay));
            best = nextWeek.atTime(startTime).atZone(zone);
        }

        return Optional.of(best.toInstant());
    }
}