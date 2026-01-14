package Pomna_Sedmica.Mindfulnes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPollingJob {

    private final ReminderEngineService engine;

    /**
     * Default: svakih 60s.
     * Možeš promijeniti u application.yml:
     * reminder.polling.delay-ms: 30000
     */
    @Scheduled(fixedDelayString = "${reminder.polling.delay-ms:60000}")
    public void run() {
        try {
            engine.tick();
        } catch (Exception ex) {
            log.error("Reminder polling failed", ex);
        }
    }
}
