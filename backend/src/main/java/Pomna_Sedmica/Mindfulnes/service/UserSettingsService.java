package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.UpdateUserSettingsRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserSettingsResponseDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;

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
     * Get user settings by ID
     */
    public UserSettingsResponseDTO getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
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
     * Check if user requires first-time actions
     */
    public boolean isFirstLoginRequired(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.isFirstLogin();
    }

    /**
     * Mark first login as completed
     */
    @Transactional
    public void completeFirstLogin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstLogin(false);
        user.setLastModifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}