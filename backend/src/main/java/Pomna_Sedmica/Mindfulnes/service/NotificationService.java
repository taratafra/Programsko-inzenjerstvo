package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.Notification;
import Pomna_Sedmica.Mindfulnes.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;

    public List<Notification> listForUser(Long userId, boolean unreadOnly) {
        if (unreadOnly) {
            return notifications.findTop50ByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        }
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notifications.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
        notifications.save(n);
    }

    @Transactional
    public void createIfNotExists(Long userId, Long scheduleId, Instant fireAt, String message) {
        // Oslanjamo se na UNIQUE constraint da spriječi duplikate.
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .scheduleId(scheduleId)
                    .fireAt(fireAt)
                    .message(message)
                    .read(false)
                    .build();
            notifications.save(n);
        } catch (Exception ignored) {
            // duplicate -> ok (već postoji)
        }
    }
}
