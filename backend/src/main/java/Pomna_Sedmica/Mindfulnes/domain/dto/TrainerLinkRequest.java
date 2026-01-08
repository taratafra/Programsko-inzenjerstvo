package Pomna_Sedmica.Mindfulnes.domain.dto;

import jakarta.validation.constraints.NotNull;

public record TrainerLinkRequest(
        @NotNull(message = "trainerId is required")
        Long trainerId,

        // ako true -> postavi kao primary odmah
        Boolean primary
) {}
