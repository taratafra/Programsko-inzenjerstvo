package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        User user = existingUser
                .map(existing -> UserMapper.updateExisting(existing, dto))
                .orElseGet(() -> UserMapper.toNewEntity(dto));

        //ključ: ovo je TRAINER servis => uvijek enforce-aj TRAINER rolu
        user.setRole(Role.TRAINER);

        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

    public List<UserDTOResponse> getAllTrainers() {

        return userRepository.findAllByRole(Role.TRAINER)
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTOResponse> getTrainerByEmail(String email) {

        return userRepository.findByEmailAndRole(email, Role.TRAINER)
                .map(UserMapper::toDTO);
    }

    public Optional<UserDTOResponse> getTrainerByAuth0Id(String auth0Id) {

        return userRepository.findByAuth0IdAndRole(auth0Id, Role.TRAINER)
                .map(UserMapper::toDTO);
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboarding(String email) {

        return userRepository.findByEmailAndRole(email, Role.TRAINER)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRequiresPasswordReset(false);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {

        return userRepository.findByAuth0IdAndRole(auth0Id, Role.TRAINER)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public User getOrCreateTrainerFromJwt(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");

        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
        }

        // 1) prvo probaj po auth0Id + TRAINER
        Optional<User> byAuth0Id = userRepository.findByAuth0Id(sub);
        if (byAuth0Id.isPresent()) {
            User user = byAuth0Id.get();

            // enforce TRAINER ako koristimo ovaj servis
            if (user.getRole() != Role.TRAINER) user.setRole(Role.TRAINER);

            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }

        // 2) ako nema po auth0Id, probaj po emailu
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                if (user.getAuth0Id() == null || user.getAuth0Id().isEmpty()) {
                    user.setAuth0Id(sub);
                }


                if (user.getRole() != Role.TRAINER) user.setRole(Role.TRAINER);

                user.setLastLogin(LocalDateTime.now());
                return userRepository.save(user);
            }
        }

        // 3) kreiraj novog TRAINER usera
        User user = new User();
        user.setAuth0Id(sub);
        user.setEmail(email);
        user.setRole(Role.TRAINER);
        user.setSocialLogin(true);
        user.setFirstLogin(true);
        user.setOnboardingComplete(false);
        user.setRequiresPasswordReset(false);
        user.setLastLogin(LocalDateTime.now());

        return userRepository.save(user);
    }
}
