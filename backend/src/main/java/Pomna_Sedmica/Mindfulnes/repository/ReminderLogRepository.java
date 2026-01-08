package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.ReminderLog;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByScheduleIdAndChannelAndReminderAt(Long scheduleId, ReminderChannel channel, Instant reminderAt);

    List<ReminderLog> findAllByUserIdOrderByReminderAtDesc(Long userId);
}
