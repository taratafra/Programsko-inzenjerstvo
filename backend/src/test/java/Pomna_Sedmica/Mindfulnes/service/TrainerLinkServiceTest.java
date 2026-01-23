package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.UserTrainer;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserTrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrainerLinkServiceTest {

    @Autowired
    private TrainerLinkService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTrainerRepository linkRepository;

    private User normalUser;
    private User trainer1;
    private User trainer2;

    @BeforeEach
    void setup() {
        linkRepository.deleteAll();
        userRepository.deleteAll();

        normalUser = new User();
        normalUser.setRole(Role.USER);
        normalUser = userRepository.save(normalUser);

        trainer1 = new User();
        trainer1.setRole(Role.TRAINER);
        trainer1 = userRepository.save(trainer1);

        trainer2 = new User();
        trainer2.setRole(Role.TRAINER);
        trainer2 = userRepository.save(trainer2);
    }

    @Test
    void shouldLinkTrainer() {
        UserTrainer link = service.linkTrainer(normalUser.getId(), trainer1.getId(), false);

        assertNotNull(link.getId());
        assertEquals(normalUser.getId(), link.getUserId());
        assertEquals(trainer1.getId(), link.getTrainerId());
        assertFalse(link.isPrimaryTrainer());
    }

    @Test
    void shouldSetPrimaryTrainerAndUnsetOldOne() {
        service.linkTrainer(normalUser.getId(), trainer1.getId(), true);
        service.linkTrainer(normalUser.getId(), trainer2.getId(), true);

        List<UserTrainer> links = linkRepository.findAllByUserId(normalUser.getId());

        assertEquals(2, links.size());

        long primaryCount = links.stream().filter(UserTrainer::isPrimaryTrainer).count();
        assertEquals(1, primaryCount);

        Long primaryTrainerId = service.getPrimaryTrainerIdOrNull(normalUser.getId());
        assertEquals(trainer2.getId(), primaryTrainerId);
    }

    @Test
    void shouldThrowIfSelectedUserIsNotTrainer() {
        User notTrainer = new User();
        notTrainer.setRole(Role.USER);
        notTrainer = userRepository.save(notTrainer);

        User finalNotTrainer = notTrainer;
        assertThrows(ResponseStatusException.class,
                () -> service.linkTrainer(normalUser.getId(), finalNotTrainer.getId(), false));
    }

    @Test
    void shouldUnlinkTrainer() {
        service.linkTrainer(normalUser.getId(), trainer1.getId(), false);

        service.unlinkTrainer(normalUser.getId(), trainer1.getId());

        List<UserTrainer> links = linkRepository.findAllByUserId(normalUser.getId());
        assertTrue(links.isEmpty());
    }

    @Test
    void shouldSetPrimaryTrainer() {
        service.linkTrainer(normalUser.getId(), trainer1.getId(), false);
        service.linkTrainer(normalUser.getId(), trainer2.getId(), false);

        service.setPrimaryTrainer(normalUser.getId(), trainer1.getId());

        Long primary = service.getPrimaryTrainerIdOrNull(normalUser.getId());
        assertEquals(trainer1.getId(), primary);
    }

    @Test
    void shouldThrowIfSettingPrimaryForNonExistingLink() {
        assertThrows(ResponseStatusException.class,
                () -> service.setPrimaryTrainer(normalUser.getId(), trainer1.getId()));
    }

    @Test
    void shouldNotThrowIfUserLinked() {
        service.linkTrainer(normalUser.getId(), trainer1.getId(), false);

        assertDoesNotThrow(() ->
                service.assertUserLinkedToTrainer(normalUser.getId(), trainer1.getId()));
    }

    @Test
    void shouldThrowIfUserNotLinked() {
        assertThrows(ResponseStatusException.class,
                () -> service.assertUserLinkedToTrainer(normalUser.getId(), trainer1.getId()));
    }
}
