package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.*;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSettingsServiceTest {

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                "settings@test.com",
                passwordEncoder.encode("oldPassword"),
                null,
                "Test",
                "User",
                LocalDate.now(),
                Role.USER,
                false
        );
        testUser.setFirstLogin(true);
        testUser.setSocialLogin(false);
        testUser.setRequiresPasswordReset(true);
        testUser.setLastModifiedAt(LocalDateTime.now());

        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldReturnUserSettings() {
        UserSettingsResponseDTO response =
                userSettingsService.getUserSettings("settings@test.com");

        assertEquals(testUser.getId(), response.id());
        assertEquals("Test", response.name());
        assertEquals("User", response.surname());
        assertTrue(response.firstLogin());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        assertThrows(
                RuntimeException.class,
                () -> userSettingsService.getUserSettings("missing@test.com")
        );
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        UpdateUserSettingsRequestDTO request =
                new UpdateUserSettingsRequestDTO(
                        "NewName",
                        null,
                        "New bio",
                        "http://image.url"
                );

        UserSettingsResponseDTO response =
                userSettingsService.updateUserSettings("settings@test.com", request);

        assertEquals("NewName", response.name());
        assertEquals("User", response.surname()); // nije promijenjeno
        assertEquals("New bio", response.bio());
        assertEquals("http://image.url", response.profilePictureUrl());
    }

    @Test
    void shouldResetPasswordOnFirstLogin() {
        FirstTimePasswordResetRequestDTO request =
                new FirstTimePasswordResetRequestDTO("newPassword");

        boolean result =
                userSettingsService.resetFirstTimePassword("settings@test.com", request);

        assertTrue(result);

        User updatedUser =
                userRepository.findByEmail("settings@test.com").orElseThrow();

        assertFalse(updatedUser.isFirstLogin());
        assertTrue(passwordEncoder.matches("newPassword", updatedUser.getPassword()));
    }

    @Test
    void shouldFailPasswordResetIfNotFirstLogin() {
        testUser.setFirstLogin(false);
        userRepository.save(testUser);

        boolean result =
                userSettingsService.resetFirstTimePassword(
                        "settings@test.com",
                        new FirstTimePasswordResetRequestDTO("newPassword")
                );

        assertFalse(result);
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordMatches() {
        ChangePasswordRequestDTO request =
                new ChangePasswordRequestDTO("oldPassword", "changedPassword");

        boolean result =
                userSettingsService.changePassword("settings@test.com", request);

        assertTrue(result);

        User updatedUser =
                userRepository.findByEmail("settings@test.com").orElseThrow();

        assertTrue(passwordEncoder.matches("changedPassword", updatedUser.getPassword()));
    }

    @Test
    void shouldFailPasswordChangeWhenCurrentPasswordIsWrong() {
        ChangePasswordRequestDTO request =
                new ChangePasswordRequestDTO("wrongPassword", "newPassword");

        boolean result =
                userSettingsService.changePassword("settings@test.com", request);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenFirstLoginAndNotSocial() {
        assertTrue(
                userSettingsService.isFirstLoginRequired("settings@test.com")
        );
    }

    @Test
    void shouldReturnFalseWhenSocialLogin() {
        testUser.setSocialLogin(true);
        userRepository.save(testUser);

        assertFalse(
                userSettingsService.isFirstLoginRequired("settings@test.com")
        );
    }
}
