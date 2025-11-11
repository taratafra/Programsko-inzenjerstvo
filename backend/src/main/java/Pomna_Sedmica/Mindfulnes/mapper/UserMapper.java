package Pomna_Sedmica.Mindfulnes.mapper;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@UtilityClass
public class UserMapper {

    public User toEntity(SaveAuth0UserRequestDTO request) {
        User user = new User();
        user.setAuth0Id(request.auth0Id());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setRole(Role.USER); // stavljamo da je ovo default
        user.setSocialLogin(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setBio("");
        user.setProfilePictureUrl("");
        return user;
    }

    public UserDTOResponse toDTO(User user) {
        return new UserDTOResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole(),
                user.isSocialLogin(),
                user.getLastLogin(),
                user.getDateOfBirth(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.isFirstLogin()
        );
    }
}
