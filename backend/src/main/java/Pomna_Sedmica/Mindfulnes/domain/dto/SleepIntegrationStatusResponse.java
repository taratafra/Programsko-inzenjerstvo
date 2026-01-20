package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.SleepIntegration;
import Pomna_Sedmica.Mindfulnes.domain.enums.SleepProvider;

import java.time.LocalDateTime;

public record SleepIntegrationStatusResponse(
        SleepProvider provider,
        boolean connected,
        String externalUserId,
        LocalDateTime connectedAt
) {
    public static SleepIntegrationStatusResponse from(SleepProvider provider, SleepIntegration integration) {
        if (integration == null) {
            return new SleepIntegrationStatusResponse(provider, false, null, null);
        }
        return new SleepIntegrationStatusResponse(
                integration.getProvider(),
                true,
                integration.getExternalUserId(),
                integration.getConnectedAt()
        );
    }
}
