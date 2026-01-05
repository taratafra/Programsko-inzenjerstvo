package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.ReminderLog;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByScheduleIdAndDueAtAndChannel(Long scheduleId, Instant dueAt, ReminderChannel channel);
}
