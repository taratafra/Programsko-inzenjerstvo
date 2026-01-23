package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateNewUser() {
        SaveAuth0UserRequestDTO dto =
                new SaveAuth0UserRequestDTO("auth0|111", "new@test.com", "New", "User", true);

        UserDTOResponse response = userService.saveOrUpdateUser(dto);

        assertNotNull(response);
        assertEquals("new@test.com", response.email());

        Optional<User> fromDb = userRepository.findByEmail("new@test.com");
        assertTrue(fromDb.isPresent());
    }

    @Test
    void shouldUpdateExistingUser() {
        User user = new User();
        user.setEmail("existing@test.com");
        user.setAuth0Id("auth0|222");
        user.setName("Old");
        user.setSurname("Name");
        user.setRole(Role.USER);

        userRepository.save(user);

        SaveAuth0UserRequestDTO dto =
                new SaveAuth0UserRequestDTO("auth0|222", "existing@test.com", "New", "Name", true);

        UserDTOResponse response = userService.saveOrUpdateUser(dto);

        assertEquals("New", response.name());

        User updated = userRepository.findByEmail("existing@test.com").orElseThrow();
        assertEquals("New", updated.getName());
    }

    @Test
    void shouldCompleteOnboarding() {
        User user = new User();
        user.setEmail("onboard@test.com");
        user.setRole(Role.USER);
        user.setOnboardingComplete(false);
        user.setRequiresPasswordReset(true);

        userRepository.save(user);

        userService.completeOnboarding("onboard@test.com");

        User updated = userRepository.findByEmail("onboard@test.com").orElseThrow();

        assertTrue(updated.isOnboardingComplete());
        assertFalse(updated.isRequiresPasswordReset());
    }

    @Test
    void shouldCreateUserFromJwt() {
        Jwt jwt = buildJwt("auth0|999", "jwt@test.com");

        User user = userService.getOrCreateUserFromJwt(jwt);

        assertNotNull(user.getId());
        assertEquals("jwt@test.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isSocialLogin());
    }

    @Test
    void shouldReturnExistingUserFromJwt() {
        User existing = new User();
        existing.setEmail("jwt2@test.com");
        existing.setAuth0Id("auth0|888");
        existing.setRole(Role.USER);

        userRepository.save(existing);

        Jwt jwt = buildJwt("auth0|888", "jwt2@test.com");

        User user = userService.getOrCreateUserFromJwt(jwt);

        assertEquals(existing.getId(), user.getId());
    }

    private Jwt buildJwt(String sub, String email) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", sub,
                        "email", email,
                        "given_name", "John",
                        "family_name", "Doe",
                        "name", "John Doe"
                )
        );
    }
}
