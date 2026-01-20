package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepIntegrationConnectRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;
import Pomna_Sedmica.Mindfulnes.repository.SleepIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SleepIntegrationService {

    private final SleepIntegrationRepository repo;

    public Optional<SleepIntegration> get(User user, SleepProvider provider) {
        return repo.findByUserIdAndProvider(user.getId(), provider);
    }

    /**
     * Connect (or update) the integration for the current user.
     *
     * This is intentionally minimal: you pass externalUserId (and optionally accessToken).
     * For a real provider, this method would handle OAuth code exchange.
     */
    public SleepIntegration connect(User user, SleepProvider provider, SleepIntegrationConnectRequest req) {
        SleepIntegration integration = repo
                .findByUserIdAndProvider(user.getId(), provider)
                .orElseGet(SleepIntegration::new);

        integration.setUser(user);
        integration.setProvider(provider);
        integration.setExternalUserId(req.externalUserId());
        integration.setAccessToken(req.accessToken());

        return repo.save(integration);
    }

    public void disconnect(User user, SleepProvider provider) {
        repo.findByUserIdAndProvider(user.getId(), provider).ifPresent(repo::delete);
    }
}
