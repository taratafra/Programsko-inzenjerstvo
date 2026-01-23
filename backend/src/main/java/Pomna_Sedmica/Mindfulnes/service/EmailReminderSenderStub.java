package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReminderSenderStub implements ReminderSender {

    private final JavaMailSender mailSender;

    // možeš i hardkodirati, ali bolje iz application.yml/properties (vidi dolje)
    //@org.springframework.beans.factory.annotation.Value("${spring.mail.username:no-reply@mindfulnes.local}")
    @Value("${app.mail.from}")
    private String from;

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.EMAIL;
    }

    @Override
    public void send(User user, String text) {
        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            log.warn("[EMAIL] User {} has no email -> skip", user.getId());
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Mindfulness Reminder");
        msg.setText(text);

        try {
            mailSender.send(msg);
            log.info("[EMAIL] ✅ Sent to={} subject={} text={}", to, msg.getSubject(), text);
        } catch (Exception e) {
            log.error("[EMAIL] ❌ Failed to send to={}", to, e);
            throw e;
        }
    }
}
