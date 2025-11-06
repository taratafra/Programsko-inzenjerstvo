package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.ChangePasswordRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.FirstTimePasswordResetRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UpdateUserSettingsRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserSettingsResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user settings by email
     */
    public UserSettingsResponseDTO getUserSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserSettingsResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.isSocialLogin(),
                user.isFirstLogin()
        );
    }

    /**
     * Update user profile settings (name, surname, bio, profile picture)
     */
    @Transactional
    public UserSettingsResponseDTO updateUserSettings(String email, UpdateUserSettingsRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if they are provided
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.surname() != null) {
            user.setSurname(request.surname());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(request.profilePictureUrl());
        }

        user.setLastModifiedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return new UserSettingsResponseDTO(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getSurname(),
                savedUser.getBio(),
                savedUser.getProfilePictureUrl(),
                savedUser.isSocialLogin(),
                savedUser.isFirstLogin()
        );
    }

    /**
     * Reset password for first-time login users (local authentication only)
     */
    @Transactional
    public boolean resetFirstTimePassword(String email, FirstTimePasswordResetRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify it's first login and user is LOCAL login (not social)
        if (!user.isFirstLogin() || user.isSocialLogin()) {
            return false;
        }

        // For first-time login, we don't verify current password, just set new one
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false); // Mark first login as completed
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    /**
     * Change password for existing local users (not first-time)
     */
    @Transactional
    public boolean changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // User must be LOCAL login (not social)
        if (user.isSocialLogin()) {
            return false;
        }

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return false;
        }

        // Update to new password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    /**
     * Check if user requires first-time password reset (local users only)
     */
    public boolean isFirstLoginRequired(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.isFirstLogin() && !user.isSocialLogin();
    }
}