package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InAppNotificationRepository
        extends JpaRepository<InAppNotification, Long> {

    // OLD METHOD - REMOVE OR KEEP FOR BACKWARD COMPATIBILITY
    // This returns ALL notifications (read and unread) - THIS IS THE PROBLEM
    List<InAppNotification>
    findAllByUserIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long userId,
            Instant now
    );

    List<InAppNotification>
    findAllByUserIdAndReadFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long userId,
            Instant now
    );

    Optional<InAppNotification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("""
        UPDATE InAppNotification n
        SET n.read = true
        WHERE n.userId = :userId AND n.id IN :ids
    """)
    void markRead(Long userId, List<Long> ids);
}