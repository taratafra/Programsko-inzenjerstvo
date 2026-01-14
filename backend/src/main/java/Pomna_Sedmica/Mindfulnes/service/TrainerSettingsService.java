package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
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
public class TrainerSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User findTrainerByEmailOrThrow(String email) {
        log.info("Searching for trainer with email: '{}'", email);

        User trainer = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    String allTrainers = userRepository.findByRole(Role.TRAINER).stream()
                            .map(u -> String.format("Trainer[id=%d, email='%s', role='%s', name='%s']",
                                    u.getId(), u.getEmail(), u.getRole(), u.getName()))
                            .collect(Collectors.joining(", "));

                    log.error("Trainer not found with email: '{}'. Available trainers: [{}]", email, allTrainers);
                    return new RuntimeException("Trainer not found with email: " + email);
                });

        // ✅ KLJUČ: mora biti TRAINER
        if (trainer.getRole() != Role.TRAINER) {
            log.error("Trainer '{}' exists but is not TRAINER (role={})", email, trainer.getRole());
            throw new RuntimeException("Forbidden: user is not a trainer");
        }

        log.info("Found trainer: id={}, email='{}', firstLogin={}, socialLogin={}",
                trainer.getId(), trainer.getEmail(), trainer.isFirstLogin(), trainer.isSocialLogin());

        return trainer;
    }

    public UserSettingsResponseDTO getTrainerSettings(String email) {
        log.info("Getting trainer settings for email: '{}'", email);
        User user = findTrainerByEmailOrThrow(email);

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

        log.info("Returning trainer settings for: '{}'", email);
        return response;
    }

    @Transactional
    public UserSettingsResponseDTO updateTrainerSettings(String email, UpdateUserSettingsRequestDTO request) {
        log.info("Updating trainer settings for email: '{}'", email);
        User trainer = findTrainerByEmailOrThrow(email);

        log.info("Update request - name: '{}', surname: '{}', bio: '{}', profilePictureUrl: '{}'",
                request.name(), request.surname(), request.bio(), request.profilePictureUrl());

        if (request.name() != null) {
            log.info("Updating name from '{}' to '{}'", trainer.getName(), request.name());
            trainer.setName(request.name());
        }
        if (request.surname() != null) {
            log.info("Updating surname from '{}' to '{}'", trainer.getSurname(), request.surname());
            trainer.setSurname(request.surname());
        }
        if (request.bio() != null) {
            log.info("Updating bio from '{}' to '{}'", trainer.getBio(), request.bio());
            trainer.setBio(request.bio());
        }
        if (request.profilePictureUrl() != null) {
            log.info("Updating profile picture URL from '{}' to '{}'",
                    trainer.getProfilePictureUrl(), request.profilePictureUrl());
            trainer.setProfilePictureUrl(request.profilePictureUrl());
        }

        trainer.setLastModifiedAt(LocalDateTime.now());
        User savedUser = userRepository.save(trainer);

        log.info("Trainer settings updated successfully for: '{}'", email);
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
        log.info("Attempting first-time password reset for trainer email: '{}'", email);
        User trainer = findTrainerByEmailOrThrow(email);

        log.info("Trainer conditions - firstLogin: {}, socialLogin: {}",
                trainer.isFirstLogin(), trainer.isSocialLogin());

        if (!trainer.isFirstLogin()) {
            log.warn("Password reset failed - not first login for trainer: '{}'", email);
            return false;
        }

        if (trainer.isSocialLogin()) {
            log.warn("Password reset failed - trainer is social login: '{}'", email);
            return false;
        }

        if (request.newPassword() == null || request.newPassword().trim().isEmpty()) {
            log.warn("Password reset failed - new password is empty for trainer: '{}'", email);
            return false;
        }

        trainer.setPassword(passwordEncoder.encode(request.newPassword()));
        trainer.setFirstLogin(false);
        trainer.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(trainer);
        log.info("First-time password reset successful for trainer: '{}'", email);
        return true;
    }

    @Transactional
    public boolean changePassword(String email, ChangePasswordRequestDTO request) {
        log.info("Attempting password change for trainer email: '{}'", email);
        User trainer = findTrainerByEmailOrThrow(email);

        if (trainer.isSocialLogin()) {
            log.warn("Password change failed - trainer is social login: '{}'", email);
            return false;
        }

        if (request.currentPassword() == null || request.newPassword() == null) {
            log.warn("Password change failed - current or new password is null for trainer: '{}'", email);
            return false;
        }

        boolean passwordMatches = passwordEncoder.matches(request.currentPassword(), trainer.getPassword());
        log.info("Current password matches: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("Password change failed - current password doesn't match for trainer: '{}'", email);
            return false;
        }

        trainer.setPassword(passwordEncoder.encode(request.newPassword()));
        trainer.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(trainer);
        log.info("Password change successful for trainer: '{}'", email);
        return true;
    }

    public boolean isFirstLoginRequired(String email) {
        log.info("Checking first login requirement for trainer email: '{}'", email);
        User trainer = findTrainerByEmailOrThrow(email);

        boolean requiresReset = trainer.isFirstLogin() && !trainer.isSocialLogin();
        log.info("First login required for '{}': {}", email, requiresReset);

        return requiresReset;
    }
}
