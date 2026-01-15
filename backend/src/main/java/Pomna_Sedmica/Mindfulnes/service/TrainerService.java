package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.mapper.TrainerMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TrainerService {

    private final UserRepository userRepository;

    @Transactional
    public UserDTOResponse saveOrUpdateTrainer(SaveAuth0UserRequestDTO dto) {
        Optional<User> existingUser = Optional.empty();

        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
            existingUser = userRepository.findByAuth0Id(dto.auth0Id());
        }

        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
            existingUser = userRepository.findByEmail(dto.email());
        }

        User user = existingUser.map(existing -> {
            return TrainerMapper.updateExisting(existing, dto);
        }).orElseGet(() -> {
            return TrainerMapper.toNewEntity(dto);
        });

        User savedUser = userRepository.save(user);
        return TrainerMapper.toDTO(savedUser);
    }


    public List<UserDTOResponse> getAllTrainers() {
        return userRepository.findAllByRole(Role.TRAINER)
                .stream()
                .map(TrainerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTOResponse> getTrainerByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(TrainerMapper::toDTO);
    }

    public Optional<UserDTOResponse> getTrainerByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .map(TrainerMapper::toDTO);
    }



    @Transactional
    public Optional<UserDTOResponse> completeOnboarding(String email) {

        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRequiresPasswordReset(false);
                    user.setRole(Role.TRAINER);
                    User savedUser = userRepository.save(user);
//log.info("Onboarding completed for user: {}", email);
                    return TrainerMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {

        return userRepository.findByAuth0Id(auth0Id)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRole(Role.TRAINER);
                    User savedUser = userRepository.save(user);
                    return TrainerMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public User getOrCreateTrainerFromJwt(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");

        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
        }

        Optional<User> byAuth0Id = userRepository.findByAuth0Id(sub);
        if (byAuth0Id.isPresent()) {
            User user = byAuth0Id.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }

        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                if (user.getAuth0Id() == null || user.getAuth0Id().isEmpty()) {
                    user.setAuth0Id(sub);
                }
                user.setLastLogin(LocalDateTime.now());
                return userRepository.save(user);
            }
        }

        User newUser = new User();
        newUser.setAuth0Id(sub);

        newUser.setEmail(email != null ? email : sub + "@placeholder.local");
        newUser.setName(jwt.getClaimAsString("given_name"));
        newUser.setSurname(jwt.getClaimAsString("family_name"));
        newUser.setRole(Role.TRAINER);
        newUser.setSocialLogin(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setFirstLogin(true);
        newUser.setOnboardingComplete(false);
        newUser.setRequiresPasswordReset(false);

        return userRepository.save(newUser);
    }
}