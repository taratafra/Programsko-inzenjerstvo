package Pomna_Sedmica.Mindfulnes.domain.dto;

public record ChangePasswordRequestDTO(
        String email,
        String currentPassword,
        String newPassword
) {
}