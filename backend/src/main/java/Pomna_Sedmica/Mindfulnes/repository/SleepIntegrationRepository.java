package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SleepIntegrationRepository extends JpaRepository<SleepIntegration, Long> {
    Optional<SleepIntegration> findByUserIdAndProvider(Long userId, SleepProvider provider);
    Optional<SleepIntegration> findByProviderAndExternalUserId(SleepProvider provider, String externalUserId);
}
