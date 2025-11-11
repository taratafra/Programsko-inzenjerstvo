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

    public User toNewEntity(SaveAuth0UserRequestDTO request) {
        User user = new User();
        user.setAuth0Id(request.auth0Id());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setRole(Role.USER); // stavljamo da je ovo default
        user.setSocialLogin(request.auth0Id() != null && !request.auth0Id().isEmpty());
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setFirstLogin(true);
        return user;
    }

    public User updateExisting(User existingUser, SaveAuth0UserRequestDTO request) {
        existingUser.setName(request.name());
        existingUser.setSurname(request.surname());
        existingUser.setAuth0Id(request.auth0Id());
        existingUser.setSocialLogin(request.isSocialLogin());
        existingUser.setLastLogin(LocalDateTime.now());
        existingUser.setFirstLogin(false); // Not first login anymore
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
                user.isFirstLogin(),
                user.getLastLogin(),
                user.getDateOfBirth()
        );
    }
}
