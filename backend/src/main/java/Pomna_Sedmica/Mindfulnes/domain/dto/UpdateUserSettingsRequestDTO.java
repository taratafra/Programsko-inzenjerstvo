package Pomna_Sedmica.Mindfulnes.domain.dto;

public record UpdateUserSettingsRequestDTO(
        String name,
        String surname,
        String bio,
        String profilePictureUrl
) {
}
