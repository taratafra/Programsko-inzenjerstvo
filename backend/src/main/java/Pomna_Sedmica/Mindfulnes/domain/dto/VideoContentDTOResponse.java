package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;

public record VideoContentDTOResponse(
        Long id,
        User owner,
        String video
) {
}
