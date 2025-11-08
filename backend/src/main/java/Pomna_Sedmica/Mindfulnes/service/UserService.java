package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserDTOResponse saveOrUpdateUser(SaveAuth0UserRequestDTO dto) {
        Optional<User> existing = dto.email() != null ?
                userRepository.findByEmail(dto.email()) :
                Optional.empty();

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            user.setLastLogin(LocalDateTime.now());
            user.setName(dto.name());
            user.setSurname(dto.surname());
            user.setAuth0Id(dto.auth0Id());
            user.setSocialLogin(dto.isSocialLogin());
        } else {
            user = new User();
            user.setEmail(dto.email());
            user.setName(dto.name());
            user.setSurname(dto.surname());
            user.setAuth0Id(dto.auth0Id());
            user.setSocialLogin(dto.isSocialLogin());
            user.setRole(Role.USER); // default role for both local and social
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    public List<UserDTOResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTOResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToDTO);
    }

    private UserDTOResponse mapToDTO(User user) {
        return new UserDTOResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole(),
                user.isSocialLogin(),
                user.getLastLogin(),
                user.getDateOfBirth()
        );
    }
}
