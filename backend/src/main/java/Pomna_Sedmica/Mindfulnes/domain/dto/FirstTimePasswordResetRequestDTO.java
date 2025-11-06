package Pomna_Sedmica.Mindfulnes.domain.dto;

public record FirstTimePasswordResetRequestDTO(
        String email,
        String newPassword
) {
}