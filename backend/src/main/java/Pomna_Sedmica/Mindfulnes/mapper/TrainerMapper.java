package Pomna_Sedmica.Mindfulnes.mapper;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class TrainerMapper {

    public Trainer toNewEntity(SaveAuth0UserRequestDTO request) {
        Trainer user = new Trainer();
        user.setAuth0Id(request.auth0Id());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setRole(Role.TRAINER); //promjena u odnosu na UserMapper
        user.setSocialLogin(request.isSocialLogin());
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setOnboardingComplete(false);
        user.setRequiresPasswordReset(false);
        user.setFirstLogin(true);
        user.setBio("");
        user.setProfilePictureUrl("");
        return user;
    }

    public Trainer updateExisting(Trainer existingUser, SaveAuth0UserRequestDTO request) {
        existingUser.setName(request.name());
        existingUser.setSurname(request.surname());
        existingUser.setAuth0Id(request.auth0Id());
        existingUser.setSocialLogin(request.isSocialLogin());
        existingUser.setLastLogin(LocalDateTime.now());
        existingUser.setFirstLogin(false);
        existingUser.setRole(Role.TRAINER);
        return existingUser;
    }

    public TrainerDTOResponse toDTO(Trainer user) {
        return new TrainerDTOResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole(),
                user.isSocialLogin(),
                user.isOnboardingComplete(),
                user.isRequiresPasswordReset(),
                user.getLastLogin(),
                user.getDateOfBirth(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.isFirstLogin(),
                user.isApproved(),
                user.getSubscribers(),
                user.getVideoContent()
        );
    }
}
