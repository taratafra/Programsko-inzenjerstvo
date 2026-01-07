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

@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public UserSettingsResponseDTO getUserSettings(String email) {
        User user = findUserByEmailOrThrow(email);

        UserSettingsResponseDTO response = new UserSettingsResponseDTO();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setSurname(user.getSurname());
        response.setBio(user.getBio());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setSocialLogin(user.isSocialLogin());
        response.setFirstLogin(user.isFirstLogin());
        response.setRequiresPasswordReset(user.isRequiresPasswordReset());

        return response;
    }

    @Transactional
    public UserSettingsResponseDTO updateUserSettings(String email, UpdateUserSettingsRequestDTO request) {
        User user = findUserByEmailOrThrow(email);

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
        
        UserSettingsResponseDTO response = new UserSettingsResponseDTO();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setName(savedUser.getName());
        response.setSurname(savedUser.getSurname());
        response.setBio(savedUser.getBio());
        response.setProfilePictureUrl(savedUser.getProfilePictureUrl());
        response.setSocialLogin(savedUser.isSocialLogin());
        response.setFirstLogin(savedUser.isFirstLogin());
        response.setRequiresPasswordReset(savedUser.isRequiresPasswordReset());
        
        return response;
    }


    @Transactional
    public boolean resetFirstTimePassword(String email, FirstTimePasswordResetRequestDTO request) {
        User user = findUserByEmailOrThrow(email);

        if (!user.isFirstLogin()) {
            return false;
        }

        if (user.isSocialLogin()) {
            return false;
        }

        if (request.newPassword() == null || request.newPassword().trim().isEmpty()) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false);
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }


    @Transactional
    public boolean changePassword(String email, ChangePasswordRequestDTO request) {
        User user = findUserByEmailOrThrow(email);

        if (user.isSocialLogin()) {
            return false;
        }

        if (request.currentPassword() == null || request.newPassword() == null) {
            return false;
        }

        boolean passwordMatches = passwordEncoder.matches(request.currentPassword(), user.getPassword());

        if (!passwordMatches) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }


    public boolean isFirstLoginRequired(String email) {
        User user = findUserByEmailOrThrow(email);

        return user.isFirstLogin() && !user.isSocialLogin();
    }
}