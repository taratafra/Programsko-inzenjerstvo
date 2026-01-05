package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import org.springframework.stereotype.Component;

@Component
public class PushReminderSenderStub implements ReminderSender {

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.PUSH;
    }

    @Override
    public void send(User user, String text) {
        // Stub: kasnije FCM/APNS/OneSignal
        System.out.println("[PUSH STUB] UserId=" + user.getId() + " | " + text);
    }
}
