package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
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

        // stvaramo ga ako ne postoji
        User newUser = UserMapper.toEntity(request);
        User savedUser = userRepository.save(newUser);
        return UserMapper.toDTO(savedUser);
    }

    public List<UserDTOResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTOResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserMapper::toDTO);
    }




    //kad zavrsi sa upitnion stavlja mu se false na first login
    @Transactional
    public Optional<UserDTOResponse> completeOnboarding(String email) {

        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setFirstLogin(false);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }


    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {

        return userRepository.findByAuth0Id(auth0Id)
                .map(user -> {
                    user.setFirstLogin(false);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }
}
