package Pomna_Sedmica.Mindfulnes.domain.dto;

public record UserSettingsResponseDTO(
        Long id,
        String email,
        String name,
        String surname,
        String bio,
        String profilePictureUrl,
        boolean isSocialLogin,
        boolean firstLogin,
        boolean requiresPasswordReset
) {
}

