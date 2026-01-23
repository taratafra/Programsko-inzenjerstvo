package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
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
class TrainerServiceTest {

    @Autowired
    private TrainerService trainerService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateNewTrainer() {
        SaveAuth0UserRequestDTO dto =
                new SaveAuth0UserRequestDTO("auth0|t1", "trainer@test.com", "Ana", "Anić", true);

        TrainerDTOResponse response = trainerService.saveOrUpdateTrainer(dto);

        assertNotNull(response);
        assertEquals("trainer@test.com", response.email());

        Optional<User> fromDb = userRepository.findByEmail("trainer@test.com");
        assertTrue(fromDb.isPresent());
        assertEquals(Role.TRAINER, fromDb.get().getRole());
    }

    @Test
    void shouldConvertUserToTrainerOnCompleteOnboarding() {
        User user = new User();
        user.setEmail("convert@test.com");
        user.setRole(Role.USER);
        user.setOnboardingComplete(false);
        user.setFirstLogin(true);

        user = userRepository.save(user);

        trainerService.completeOnboarding("convert@test.com");

        User updated = userRepository.findByEmail("convert@test.com").orElseThrow();

        assertEquals(Role.TRAINER, updated.getRole());
        assertTrue(updated.isOnboardingComplete());
        assertFalse(updated.isFirstLogin());

        assertInstanceOf(Trainer.class, updated);
    }

    @Test
    void shouldReturnTrainerByEmail() {
        Trainer trainer = new Trainer();
        trainer.setEmail("existing@trainer.com");
        trainer.setRole(Role.TRAINER);

        userRepository.save(trainer);

        Optional<TrainerDTOResponse> result =
                trainerService.getTrainerByEmail("existing@trainer.com");

        assertTrue(result.isPresent());
        assertEquals("existing@trainer.com", result.get().email());
    }

    @Test
    void shouldCreateTrainerFromJwt() {
        Jwt jwt = buildJwt("auth0|999", "jwttrainer@test.com");

        User user = trainerService.getOrCreateTrainerFromJwt(jwt);

        assertNotNull(user.getId());
        assertEquals(Role.TRAINER, user.getRole());
        assertTrue(user.isSocialLogin());
    }

    @Test
    void shouldThrowIfBannedTrainerLogsIn() {
        User banned = new User();
        banned.setEmail("banned@test.com");
        banned.setAuth0Id("auth0|ban");
        banned.setRole(Role.TRAINER);
        banned.setBanned(true);

        userRepository.save(banned);

        Jwt jwt = buildJwt("auth0|ban", "banned@test.com");

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> trainerService.getOrCreateTrainerFromJwt(jwt)
        );
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
                        "given_name", "Marko",
                        "family_name", "Marić",
                        "name", "Marko Marić"
                )
        );
    }
}
