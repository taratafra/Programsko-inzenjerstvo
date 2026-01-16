package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.Video;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record TrainerDTOResponse(
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
        Boolean firstLogin,
        Boolean approved,
        List<User> subscribers,
        List<Video> videoContent
) {
}
