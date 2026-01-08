package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Email sender stub:
 * - za lab/test: samo logira "poslani email"
 * - kasnije se zamijeni pravim SMTP senderom (Mailtrap/Gmail/SendGrid)
 */
@Slf4j
@Service
public class EmailReminderSenderStub implements ReminderSender {

    @Override
    public void send(User user, PracticeSchedule schedule, Instant occurrenceStartAt) {
        log.info("[EMAIL STUB] To={} | Practice='{}' | startsAt={}",
                user.getEmail(), schedule.getTitle(), occurrenceStartAt);
    }
}
