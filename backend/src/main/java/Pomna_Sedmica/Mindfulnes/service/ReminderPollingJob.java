package Pomna_Sedmica.Mindfulnes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderPollingJob {

    private final ReminderEngineService engine;

    // svake 30 sekundi (možeš 60s ako želiš)
    @Scheduled(fixedDelayString = "${reminders.pollDelayMs:30000}")
    public void poll() {
        int sent = engine.pollAndSend(60); // gledaj sljedećih 60 sekundi
        if (sent > 0) {
            System.out.println("[ReminderPollingJob] sent=" + sent);
        }
    }
}
