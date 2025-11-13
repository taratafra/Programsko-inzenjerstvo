package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record UserDTOResponse(
        Long id,
        String name,
        String surname,
        String email,
        Role role,
        Boolean isSocialLogin,
        Boolean isOnboardingComplete,
        Boolean requiresPasswordReset,
        LocalDateTime lastLogin,
        LocalDate dateOfBirth,
        String bio,
        String profilePictureUrl,
        Boolean firstLogin
) {}
