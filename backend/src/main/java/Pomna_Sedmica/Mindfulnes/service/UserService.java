//package Pomna_Sedmica.Mindfulnes.service;
//
//import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
//import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
//import Pomna_Sedmica.Mindfulnes.domain.entity.User;
//import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
//import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
//import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.http.HttpStatus;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@RequiredArgsConstructor
//@Service
//public class UserService {
//
//    private final UserRepository userRepository;
//
//    @Transactional
//    public UserDTOResponse saveOrUpdateUser(SaveAuth0UserRequestDTO dto) {
//        Optional<User> existingUser = Optional.empty();
//
//        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
//            existingUser = userRepository.findByAuth0Id(dto.auth0Id());
//        }
//
//        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
//            existingUser = userRepository.findByEmail(dto.email());
//        }
//
//        User user = existingUser.map(existing -> {
//            return UserMapper.updateExisting(existing, dto);
//        }).orElseGet(() -> {
//            return UserMapper.toNewEntity(dto);
//        });
//
//        User savedUser = userRepository.save(user);
//        return UserMapper.toDTO(savedUser);
//    }
//
//
//    public List<UserDTOResponse> getAllUsers() {
//        return userRepository.findAll()
//                .stream()
//                .map(UserMapper::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    public Optional<UserDTOResponse> getUserByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .map(UserMapper::toDTO);
//    }
//
//    public Optional<UserDTOResponse> getUserByAuth0Id(String auth0Id) {
//        return userRepository.findByAuth0Id(auth0Id)
//                .map(UserMapper::toDTO);
//    }
//
//
//
//    @Transactional
//    public Optional<UserDTOResponse> completeOnboarding(String email) {
//
//        return userRepository.findByEmail(email)
//                .map(user -> {
//                    user.setOnboardingComplete(true);
//                    user.setRequiresPasswordReset(false);
//                    User savedUser = userRepository.save(user);
//                    //log.info("Onboarding completed for user: {}", email);
//                    return UserMapper.toDTO(savedUser);
//                });
//    }
//
//    @Transactional
//    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {
//
//        return userRepository.findByAuth0Id(auth0Id)
//                .map(user -> {
//                    user.setOnboardingComplete(true);
//                    User savedUser = userRepository.save(user);
//                    return UserMapper.toDTO(savedUser);
//                });
//    }
//
//    @Transactional
//    public User getOrCreateUserFromJwt(Jwt jwt) {
//        String sub = jwt.getClaimAsString("sub");
//        String email = jwt.getClaimAsString("email");
//
//        if (sub == null || sub.isBlank()) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
//        }
//
//        Optional<User> byAuth0Id = userRepository.findByAuth0Id(sub);
//        if (byAuth0Id.isPresent()) {
//            User user = byAuth0Id.get();
//            user.setLastLogin(LocalDateTime.now());
//            return userRepository.save(user);
//        }
//
//        if (email != null && !email.isBlank()) {
//            Optional<User> byEmail = userRepository.findByEmail(email);
//            if (byEmail.isPresent()) {
//                User user = byEmail.get();
//                if (user.getAuth0Id() == null || user.getAuth0Id().isEmpty()) {
//                    user.setAuth0Id(sub);
//                }
//                user.setLastLogin(LocalDateTime.now());
//                return userRepository.save(user);
//            }
//        }
//        String givenName = jwt.getClaimAsString("given_name");
//        String familyName = jwt.getClaimAsString("family_name");
//        String fullName = jwt.getClaimAsString("name");
//
//        String firstName;
//        String lastName;
//
//        // If Auth0 provides given_name and family_name, use those
//        if (givenName != null && !givenName.isBlank() && familyName != null && !familyName.isBlank()) {
//            firstName = givenName;
//            lastName = familyName;
//        }
//        // If only given_name is provided, use it as first name
//        else if (givenName != null && !givenName.isBlank()) {
//            firstName = givenName;
//            lastName = familyName != null ? familyName : "";
//        }
//        // Otherwise, try to split the full name
//        else if (fullName != null && !fullName.isBlank()) {
//            if (fullName.contains(" ")) {
//                String[] nameParts = fullName.trim().split("\\s+", 2);
//                firstName = nameParts[0];
//                lastName = nameParts.length > 1 ? nameParts[1] : "";
//            } else {
//                firstName = fullName;
//                lastName = "";
//            }
//        }
//        // Fallback
//        else {
//            firstName = "User";
//            lastName = "";
//        }
//
//
//
//        User newUser = new User();
//        newUser.setAuth0Id(sub);
//
//        newUser.setEmail(email != null ? email : sub + "@placeholder.local");
//        newUser.setName(firstName);
//        newUser.setSurname(lastName);
//        newUser.setRole(Role.USER);
//        newUser.setSocialLogin(true);
//        newUser.setCreatedAt(LocalDateTime.now());
//        newUser.setLastLogin(LocalDateTime.now());
//        newUser.setFirstLogin(true);
//        newUser.setOnboardingComplete(false);
//        newUser.setRequiresPasswordReset(false);
//
//        return userRepository.save(newUser);
//    }
//
//}
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

        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
            existingUser = userRepository.findByAuth0Id(dto.auth0Id());
        }

        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
            existingUser = userRepository.findByEmail(dto.email());
        }

        User user = existingUser.map(existing -> {
            return UserMapper.updateExisting(existing, dto);
        }).orElseGet(() -> {
            return UserMapper.toNewEntity(dto);
        });

        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

    // ADD THIS METHOD - Missing from original
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public List<UserDTOResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ADD THIS METHOD - For getting all trainers
    public List<User> getAllTrainers() {
        return userRepository.findAllByRole(Role.TRAINER);
    }

    // ADD THIS METHOD - For getting user by ID (needed for frontend)
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
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
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    user.setRequiresPasswordReset(false);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public Optional<UserDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .map(user -> {
                    user.setOnboardingComplete(true);
                    User savedUser = userRepository.save(user);
                    return UserMapper.toDTO(savedUser);
                });
    }

    @Transactional
    public User getOrCreateUserFromJwt(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");

        System.out.println("DEBUG: getOrCreateUserFromJwt - sub: " + sub + ", email: " + email);
        System.out.println("DEBUG: Claims: " + jwt.getClaims());

        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
        }

        // If email claim is missing but sub looks like an email (local JWT), use sub as email
        if ((email == null || email.isBlank()) && sub.contains("@")) {
            email = sub;
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

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String fullName = jwt.getClaimAsString("name");

        String firstName;
        String lastName;

        if (givenName != null && !givenName.isBlank() && familyName != null && !familyName.isBlank()) {
            firstName = givenName;
            lastName = familyName;
        } else if (givenName != null && !givenName.isBlank()) {
            firstName = givenName;
            lastName = familyName != null ? familyName : "";
        } else if (fullName != null && !fullName.isBlank()) {
            if (fullName.contains(" ")) {
                String[] nameParts = fullName.trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            } else {
                firstName = fullName;
                lastName = "";
            }
        } else {
            firstName = "User";
            lastName = "";
        }

        User newUser = new User();
        newUser.setAuth0Id(sub);
        newUser.setEmail(email != null ? email : sub + "@placeholder.local");
        newUser.setName(firstName);
        newUser.setSurname(lastName);
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