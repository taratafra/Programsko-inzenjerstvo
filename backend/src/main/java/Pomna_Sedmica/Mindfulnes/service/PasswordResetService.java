package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.ChangePasswordRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.FirstTimePasswordResetRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Reset password for first-time login users
     */
    @Transactional
    public boolean resetFirstTimePassword(FirstTimePasswordResetRequestDTO request) {
        Optional<User> userOptional = userRepository.findByEmail(request.email());

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        // Verify it's first login and user is local login
        if (!user.isFirstLogin() || user.isSocialLogin()) {
            return false;
        }

        // Update to new password and mark first login as completed
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false); // First login completed
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    /**
     * Change password for existing users (not first-time)
     */
    @Transactional
    public boolean changePassword(ChangePasswordRequestDTO request) {
        Optional<User> userOptional = userRepository.findByEmail(request.email());

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        // User must be local login
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
     * Check if user requires first-time password reset
     */
    public boolean isFirstLoginRequired(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        return user.isFirstLogin() && !user.isSocialLogin();
    }
}