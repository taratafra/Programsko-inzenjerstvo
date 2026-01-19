package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.InAppNotification;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import Pomna_Sedmica.Mindfulnes.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushReminderSenderStub implements ReminderSender {

    private final InAppNotificationRepository notificationRepository;

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.PUSH;
    }

    @Override
    public void send(User user, String text) {
        try {
            InAppNotification notification = InAppNotification.builder()
                    .userId(user.getId())
                    .title("Practice Reminder")
                    .message(text)
                    .createdAt(Instant.now())
                    .scheduledStartAt(null)
                    .read(false)
                    .build();

            notificationRepository.save(notification);

            log.info("[PUSH] In-app notification created for userId={}, message={}", user.getId(), text);
        } catch (Exception e) {
            log.error("[PUSH] Failed to create in-app notification for userId={}", user.getId(), e);
            throw new RuntimeException("Failed to create in-app notification", e);
        }
    }
}