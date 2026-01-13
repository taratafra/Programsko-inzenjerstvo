package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<InAppNotification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
