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

    // “kanali” (email + in-app)
    private final EmailReminderSenderStub emailSender;
    private final PushReminderSenderStub pushSender;

    /**
     * Polling metoda: poziva se svakih N sekundi iz ReminderPollingJob.
     * Gleda sve enabled schedule-e i šalje reminder ako je "due".
     */
    public void pollAndSend() {
        Instant now = Instant.now();

        // Window tolerancija: ako job kasni malo, i dalje ćemo uhvatiti reminder
        Duration window = Duration.ofSeconds(75);
        Instant windowEnd = now.plus(window);

        List<PracticeSchedule> enabled = schedules.findByEnabledTrue();
        for (PracticeSchedule s : enabled) {
            if (!s.isEnabled()) continue;

            Optional<Instant> occOpt = computeNextOccurrenceStart(s, now);
            if (occOpt.isEmpty()) continue;

            Instant occurrenceStartAt = occOpt.get();
            int minutesBefore = s.getReminderMinutesBefore() == null ? 10 : s.getReminderMinutesBefore();
            Instant reminderAt = occurrenceStartAt.minus(Duration.ofMinutes(minutesBefore));

            // reminder je "due" ako je unutar polling prozora
            if (reminderAt.isBefore(now) || reminderAt.isAfter(windowEnd)) {
                continue;
            }

            // user mora postojati
            Optional<User> userOpt = users.findById(s.getUserId());
            if (userOpt.isEmpty()) continue;
            User u = userOpt.get();

            // šaljemo oba kanala (email + in-app)
            sendOnce(s, u, ReminderChannel.EMAIL, reminderAt, occurrenceStartAt);
            sendOnce(s, u, ReminderChannel.PUSH, reminderAt, occurrenceStartAt);
        }
    }

    private void sendOnce(PracticeSchedule s,
                          User u,
                          ReminderChannel channel,
                          Instant reminderAt,
                          Instant occurrenceStartAt) {

        if (logs.existsByScheduleIdAndChannelAndReminderAt(s.getId(), channel, reminderAt)) {
            return; // već poslano (ili pokušano)
        }

        ReminderLog log = ReminderLog.builder()
                .userId(u.getId())
                .scheduleId(s.getId())
                .channel(channel)
                .status(ReminderStatus.PENDING)
                .reminderAt(reminderAt)
                .build();

        log = logs.save(log);

        try {
            if (channel == ReminderChannel.EMAIL) {
                emailSender.send(u, s, occurrenceStartAt);
            } else {
                pushSender.send(u, s, occurrenceStartAt);
            }

            log.setStatus(ReminderStatus.SENT);
            log.setSentAt(Instant.now());
            log.setErrorMessage(null);
            logs.save(log);

        } catch (Exception ex) {
            log.setStatus(ReminderStatus.FAILED);
            log.setSentAt(Instant.now());
            log.setErrorMessage(ex.getMessage());
            logs.save(log);
        }
    }

    /**
     * Izračun “sljedećeg starta” termina u vremenskoj zoni schedule-a.
     * DAILY: danas u startTime ako nije prošlo; inače sutra.
     * WEEKLY: najbliži idući od daysOfWeek (u max 7 dana).
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

        if (s.getRepeatType() == RepeatType.DAILY) {
            ZonedDateTime today = now.toLocalDate().atTime(startTime).atZone(zone);
            ZonedDateTime next = today.isAfter(now) ? today : today.plusDays(1);
            return Optional.of(next.toInstant());
        }

        // WEEKLY
        Set<DayOfWeek> days = s.getDaysOfWeek();
        if (days == null || days.isEmpty()) return Optional.empty();

        // pogledaj od danas do +7 dana
        ZonedDateTime best = null;
        for (int i = 0; i <= 7; i++) {
            LocalDate d = now.toLocalDate().plusDays(i);
            if (!days.contains(d.getDayOfWeek())) continue;

            ZonedDateTime cand = d.atTime(startTime).atZone(zone);
            if (!cand.isAfter(now)) continue;

            if (best == null || cand.isBefore(best)) best = cand;
        }

        // Ako ništa u idućih 7 dana (npr. sad je nakon vremena na zadani dan),
        // uzmi sljedeći tjedan za najbliži dan.
        if (best == null) {
            DayOfWeek minDay = days.stream().min(Comparator.naturalOrder()).orElse(null);
            if (minDay == null) return Optional.empty();
            LocalDate nextWeek = now.toLocalDate().with(TemporalAdjusters.next(minDay));
            best = nextWeek.atTime(startTime).atZone(zone);
        }

        return Optional.of(best.toInstant());
    }
}
