package Pomna_Sedmica.Mindfulnes.domain.dto;

public record ChangePasswordRequestDTO(
        String currentPassword,
        String newPassword
        ) {}