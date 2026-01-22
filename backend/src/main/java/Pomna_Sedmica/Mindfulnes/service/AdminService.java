package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.mapper.AdminMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.repository.TrainerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;

    @Transactional
    public UserDTOResponse saveOrUpdateAdmin(SaveAuth0UserRequestDTO dto) {
        Optional<User> existingUser = Optional.empty();

        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
            existingUser = userRepository.findByAuth0Id(dto.auth0Id());
        }

        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
            existingUser = userRepository.findByEmail(dto.email());
        }

        User user = existingUser.map(existing -> {
            return AdminMapper.updateExisting(existing, dto);
        }).orElseGet(() -> {
            return AdminMapper.toNewEntity(dto);
        });

        User savedUser = userRepository.save(user);
        return AdminMapper.toDTO(savedUser);
    }


    public List<UserDTOResponse> getAllAdmins() {
        return userRepository.findAllByRole(Role.ADMIN)
                .stream()
                .map(AdminMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTOResponse> getAdminByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(AdminMapper::toDTO);
    }

    public Optional<UserDTOResponse> getAdminByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .map(AdminMapper::toDTO);
    }



    @Transactional
    public Optional<UserDTOResponse> completeOnboarding(String email) {

        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRequiresPasswordReset(false);
                    User savedUser = userRepository.save(user);
                    //log.info("Onboarding completed for user: {}", email);
                    return AdminMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {

        return userRepository.findByAuth0Id(auth0Id)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    User savedUser = userRepository.save(user);
                    return AdminMapper.toDTO(savedUser);
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

            // Check if user is banned
            if (user.isBanned()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been banned");
            }

            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }

        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();

                // Check if user is banned
                if (user.isBanned()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been banned");
                }

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
        newUser.setRole(Role.ADMIN);
        newUser.setSocialLogin(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setFirstLogin(true);
        newUser.setOnboardingComplete(false);
        newUser.setRequiresPasswordReset(false);

        return userRepository.save(newUser);
    }


    @Transactional
    public void setUserBanStatus(Long userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setBanned(banned);
        userRepository.save(user);
    }

    @Transactional
    public void setTrainerBanStatus(Long trainerId, boolean banned) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));

        trainer.setBanned(banned);
        trainerRepository.save(trainer);
    }
}