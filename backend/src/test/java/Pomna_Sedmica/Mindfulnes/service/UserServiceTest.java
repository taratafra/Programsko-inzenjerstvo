package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;

    @Test
    void saveAndGetUser() {
        User user = new User(
                "user@user",
                passwordEncoder.encode("admin"),
                null,
                "user",
                "user",
                LocalDate.now(),
                Role.USER,
                false
        );
        //userService.saveUser(user);
        UserDTOResponse response = userService.getUserByEmail("user@user").orElse(null);
        assertEquals("user@user", response.email());
    }

    @Test
    void createAndGetUserFromJwt() {
        //
    }

    @Test
    void getMultipleUsers() {
        //
    }

    @Test
    void saveOrUpdateUser() {
    }

    @Test
    void saveUser() {
    }

    @Test
    void getAllUsers() {
    }

    @Test
    void getAllTrainers() {
    }

    @Test
    void getUserById() {
    }

    @Test
    void getUserByEmail() {
    }

    @Test
    void getUserByAuth0Id() {
    }

    @Test
    void completeOnboarding() {
    }

    @Test
    void completeOnboardingByAuth0Id() {
    }

    @Test
    void getOrCreateUserFromJwt() {
    }
}