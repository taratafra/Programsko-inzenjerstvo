package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User findUserByEmailOrThrow(String email) {
        log.info("Searching for user with email: '{}'", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    String allUsers = userRepository.findAll().stream()
                            .map(u -> String.format("User[id=%d, email='%s', name='%s']",
                                    u.getId(), u.getEmail(), u.getName()))
                            .collect(Collectors.joining(", "));

                    log.error("User not found with email: '{}'. Available users: [{}]", email, allUsers);
                    return new RuntimeException("User not found with email: " + email);
                });

        log.info("Found user: id={}, email='{}', firstLogin={}, socialLogin={}",
                user.getId(), user.getEmail(), user.isFirstLogin(), user.isSocialLogin());
        return user;
    }

    public UserSettingsResponseDTO getUserSettings(String email) {
        log.info("Getting user settings for email: '{}'", email);
        User user = findUserByEmailOrThrow(email);

        UserSettingsResponseDTO response = new UserSettingsResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.isSocialLogin(),
                user.isFirstLogin(),
                user.isRequiresPasswordReset()
        );

        log.info("Returning user settings for: '{}'", email);
        return response;
    }

    @Transactional
    public UserSettingsResponseDTO updateUserSettings(String email, UpdateUserSettingsRequestDTO request) {
        log.info("Updating user settings for email: '{}'", email);
        User user = findUserByEmailOrThrow(email);

        log.info("Update request - name: '{}', surname: '{}', bio: '{}', profilePictureUrl: '{}'",
                request.name(), request.surname(), request.bio(), request.profilePictureUrl());

        if (request.name() != null) {
            log.info("Updating name from '{}' to '{}'", user.getName(), request.name());
            user.setName(request.name());
        }
        if (request.surname() != null) {
            log.info("Updating surname from '{}' to '{}'", user.getSurname(), request.surname());
            user.setSurname(request.surname());
        }
        if (request.bio() != null) {
            log.info("Updating bio from '{}' to '{}'", user.getBio(), request.bio());
            user.setBio(request.bio());
        }
        if (request.profilePictureUrl() != null) {
            log.info("Updating profile picture URL from '{}' to '{}'",
                    user.getProfilePictureUrl(), request.profilePictureUrl());
            user.setProfilePictureUrl(request.profilePictureUrl());
        }

        user.setLastModifiedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        log.info("User settings updated successfully for: '{}'", email);
        return new UserSettingsResponseDTO(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getSurname(),
                savedUser.getBio(),
                savedUser.getProfilePictureUrl(),
                savedUser.isSocialLogin(),
                savedUser.isFirstLogin(),
                savedUser.isRequiresPasswordReset()
        );
    }


    @Transactional
    public boolean resetFirstTimePassword(String email, FirstTimePasswordResetRequestDTO request) {
        log.info("Attempting first-time password reset for email: '{}'", email);
        User user = findUserByEmailOrThrow(email);

        log.info("User conditions - firstLogin: {}, socialLogin: {}",
                user.isFirstLogin(), user.isSocialLogin());

        if (!user.isFirstLogin()) {
            log.warn("Password reset failed - not first login for user: '{}'", email);
            return false;
        }

        if (user.isSocialLogin()) {
            log.warn("Password reset failed - user is social login: '{}'", email);
            return false;
        }

        if (request.newPassword() == null || request.newPassword().trim().isEmpty()) {
            log.warn("Password reset failed - new password is empty for user: '{}'", email);
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false);
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("First-time password reset successful for user: '{}'", email);
        return true;
    }


    @Transactional
    public boolean changePassword(String email, ChangePasswordRequestDTO request) {
        log.info("Attempting password change for email: '{}'", email);
        User user = findUserByEmailOrThrow(email);

        if (user.isSocialLogin()) {
            log.warn("Password change failed - user is social login: '{}'", email);
            return false;
        }

        if (request.currentPassword() == null || request.newPassword() == null) {
            log.warn("Password change failed - current or new password is null for user: '{}'", email);
            return false;
        }

        boolean passwordMatches = passwordEncoder.matches(request.currentPassword(), user.getPassword());
        log.info("Current password matches: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("Password change failed - current password doesn't match for user: '{}'", email);
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Password change successful for user: '{}'", email);
        return true;
    }


    public boolean isFirstLoginRequired(String email) {
        log.info("Checking first login requirement for email: '{}'", email);
        User user = findUserByEmailOrThrow(email);

        boolean requiresReset = user.isFirstLogin() && !user.isSocialLogin();
        log.info("First login required for '{}': {}", email, requiresReset);

        return requiresReset;
    }
}