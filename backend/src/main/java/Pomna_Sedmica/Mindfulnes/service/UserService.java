package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
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
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDTOResponse saveOrUpdateUser(SaveAuth0UserRequestDTO dto) {
        Optional<User> existingUser = Optional.empty();

        // Try to find by Auth0 ID first
        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
            existingUser = userRepository.findByAuth0Id(dto.auth0Id());
        }

        // Fallback: Try to find by email
        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
            existingUser = userRepository.findByEmail(dto.email());
        }

        User user = existingUser.map(existing -> {
            // Update existing user
            return UserMapper.updateExisting(existing, dto);
        }).orElseGet(() -> {
            // Create new user
            return UserMapper.toNewEntity(dto);
        });

        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

//    @Transactional
//    public UserDTOResponse saveOrUpdateUser(SaveAuth0UserRequestDTO dto) {
//
//        Optional<User> existingUser = dto.email() != null
//                ? userRepository.findByEmail(dto.email())
//                : Optional.empty();
//
//        User user;
//
//        if (existingUser.isPresent()) {
//            user = UserMapper.updateExisting(existingUser.get(), dto);
//        } else {
//            user = UserMapper.toNewEntity(dto);
//        }
//
//        User savedUser = userRepository.save(user);
//        return UserMapper.toDTO(savedUser);
//    }

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

    public Optional<UserDTOResponse> getUserByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .map(UserMapper::toDTO);
    }



    @Transactional
    public Optional<UserDTOResponse> completeOnboarding(String email) {
        //log.info("Completing onboarding for user: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRequiresPasswordReset(false);
                    User savedUser = userRepository.save(user);
                    //log.info("Onboarding completed for user: {}", email);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {
        //log.info("Completing onboarding for Auth0 user: {}", auth0Id);

        return userRepository.findByAuth0Id(auth0Id)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    User savedUser = userRepository.save(user);
                    //log.info("Onboarding completed for Auth0 user: {}", auth0Id);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public User getOrCreateUserFromJwt(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");

        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
        }

        // 1️⃣ Try by Auth0 ID first
        Optional<User> byAuth0Id = userRepository.findByAuth0Id(sub);
        if (byAuth0Id.isPresent()) {
            User user = byAuth0Id.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }

        // 2️⃣ Fallback: find by email and link Auth0 ID
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

        // 3️⃣ Create new user if not found
        User newUser = new User();
        newUser.setAuth0Id(sub);

//        String finalEmail;
//        if (email != null && !email.isBlank()) {
//            finalEmail = email;
//        } else if (sub.contains("@")) {
//            finalEmail = sub; // sub is already email-like
//        } else {
//            finalEmail = sub + "@placeholder.local";
//        }
//        newUser.setEmail(finalEmail);

        newUser.setEmail(email != null ? email : sub + "@placeholder.local");
        newUser.setName(jwt.getClaimAsString("given_name"));
        newUser.setSurname(jwt.getClaimAsString("family_name"));
        newUser.setRole(Role.USER);
        newUser.setSocialLogin(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setFirstLogin(true);
        newUser.setOnboardingComplete(false);
        newUser.setRequiresPasswordReset(false);

        return userRepository.save(newUser);
    }
}
