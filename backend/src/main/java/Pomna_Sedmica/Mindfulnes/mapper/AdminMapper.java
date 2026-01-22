package Pomna_Sedmica.Mindfulnes.mapper;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class AdminMapper {

    public User toNewEntity(SaveAuth0UserRequestDTO request) {
        User user = new User();
        user.setAuth0Id(request.auth0Id());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setRole(Role.ADMIN); //promjena u odnosu na UserMapper
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

    public User updateExisting(User existingUser, SaveAuth0UserRequestDTO request) {
        existingUser.setName(request.name());
        existingUser.setSurname(request.surname());
        existingUser.setAuth0Id(request.auth0Id());
        existingUser.setSocialLogin(request.isSocialLogin());
        existingUser.setLastLogin(LocalDateTime.now());
        existingUser.setFirstLogin(false);
        return existingUser;
    }

    public UserDTOResponse toDTO(User user) {
        return new UserDTOResponse(
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
                user.isBanned()
        );
    }
}
