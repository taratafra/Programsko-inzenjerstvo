package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.InAppNotification;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticeSchedule;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * "In-app push": spremamo notifikaciju u bazu (frontend je prikazuje).
 * Nije Web Push (FCM) nego jednostavnije i dovoljno za lab.
 */
@Service
@RequiredArgsConstructor
public class PushReminderSenderStub implements ReminderSender {

    private final InAppNotificationRepository notifications;

    @Override
    public void send(User user, PracticeSchedule schedule, Instant occurrenceStartAt) {
        String title = "Practice reminder";
        String msg = "Your practice \"" + schedule.getTitle() + "\" starts soon.";

        InAppNotification n = InAppNotification.builder()
                .userId(user.getId())
                .title(title)
                .message(msg)
                .createdAt(Instant.now())
                .scheduledStartAt(occurrenceStartAt)
                .read(false)
                .build();

        notifications.save(n);
    }
}
