package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushReminderSenderStub implements ReminderSender {

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.PUSH;
    }

    @Override
    public void send(User user, String text) {
        // za sad samo log (in-app push ćeš kasnije mapirati na DB tablicu/endpoint)
        log.info("[PUSH-STUB] Would notify userId={} text={}", user.getId(), text);
    }
}
