package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.repository.TrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @BeforeEach
    void cleanDb() {
        trainerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateNewAdmin() {
        SaveAuth0UserRequestDTO dto =
                new SaveAuth0UserRequestDTO("auth0|admin1", "admin@test.com", "Marko", "Marić", true);

        UserDTOResponse response = adminService.saveOrUpdateAdmin(dto);

        assertNotNull(response);
        assertEquals("admin@test.com", response.email());

        User saved = userRepository.findByEmail("admin@test.com").orElseThrow();
        assertEquals(Role.ADMIN, saved.getRole());
    }

    @Test
    void shouldCreateAdminFromJwtIfNotExists() {
        Jwt jwt = buildJwt("auth0|newAdmin", "jwtadmin@test.com");

        User user = adminService.getOrCreateTrainerFromJwt(jwt);

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("jwtadmin@test.com", user.getEmail());
    }

    @Test
    void shouldUpdateLastLoginIfAdminExists() {
        User existing = new User();
        existing.setAuth0Id("auth0|exists");
        existing.setEmail("exists@test.com");
        existing.setRole(Role.ADMIN);
        userRepository.save(existing);

        Jwt jwt = buildJwt("auth0|exists", "exists@test.com");

        User updated = adminService.getOrCreateTrainerFromJwt(jwt);

        assertNotNull(updated.getLastLogin());
    }

    @Test
    void shouldThrowIfUserIsBanned() {
        User banned = new User();
        banned.setAuth0Id("auth0|banned");
        banned.setEmail("banned@test.com");
        banned.setRole(Role.ADMIN);
        banned.setBanned(true);
        userRepository.save(banned);

        Jwt jwt = buildJwt("auth0|banned", "banned@test.com");

        assertThrows(ResponseStatusException.class,
                () -> adminService.getOrCreateTrainerFromJwt(jwt));
    }

    @Test
    void shouldCompleteOnboarding() {
        User user = new User();
        user.setEmail("onboard@test.com");
        user.setRole(Role.ADMIN);
        user.setOnboardingComplete(false);
        userRepository.save(user);

        Optional<UserDTOResponse> result =
                adminService.completeOnboarding("onboard@test.com");

        assertTrue(result.isPresent());

        User updated = userRepository.findByEmail("onboard@test.com").orElseThrow();
        assertTrue(updated.isOnboardingComplete());
        assertFalse(updated.isRequiresPasswordReset());
    }

    @Test
    void shouldBanUser() {
        User user = new User();
        user.setEmail("ban@test.com");
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        adminService.setUserBanStatus(user.getId(), true);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updated.isBanned());
    }

    @Test
    void shouldThrowIfUserNotFoundWhenBanning() {
        assertThrows(ResponseStatusException.class,
                () -> adminService.setUserBanStatus(999L, true));
    }

    private Jwt buildJwt(String sub, String email) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", sub,
                        "email", email,
                        "given_name", "Ime",
                        "family_name", "Prezime"
                )
        );
    }
}
