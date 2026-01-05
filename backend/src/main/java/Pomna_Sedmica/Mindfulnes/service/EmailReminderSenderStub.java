package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import org.springframework.stereotype.Component;

@Component
public class EmailReminderSenderStub implements ReminderSender {

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.EMAIL;
    }

    @Override
    public void send(User user, String text) {
        // Stub: kasnije zamijeniš pravim email providerom
        System.out.println("[EMAIL STUB] To=" + user.getEmail() + " | " + text);
    }
}
