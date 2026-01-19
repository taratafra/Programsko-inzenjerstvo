package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.ReminderLog;
import Pomna_Sedmica.Mindfulnes.domain.enums.ReminderChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByScheduleIdAndChannelAndReminderAt(Long scheduleId,
                                                      ReminderChannel channel,
                                                      Instant reminderAt);

    boolean existsByScheduleIdAndChannelAndOccurrenceStartAt(
            Long scheduleId,
            ReminderChannel channel,
            Instant occurrenceStartAt
    );

    List<ReminderLog> findByScheduleIdOrderByReminderAtDesc(Long scheduleId);

    // ✅ NEW: More robust check that looks for ANY log within a time window
    @Query("""
        SELECT COUNT(rl) > 0 
        FROM ReminderLog rl 
        WHERE rl.scheduleId = :scheduleId 
        AND rl.channel = :channel 
        AND rl.occurrenceStartAt = :occurrenceStartAt
    """)
    boolean hasReminderBeenSent(
            @Param("scheduleId") Long scheduleId,
            @Param("channel") ReminderChannel channel,
            @Param("occurrenceStartAt") Instant occurrenceStartAt
    );
}