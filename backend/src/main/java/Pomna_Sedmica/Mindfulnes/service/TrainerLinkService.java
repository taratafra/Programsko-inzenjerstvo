package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.entity.UserTrainer;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserTrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerLinkService {

    private final UserTrainerRepository links;
    private final UserRepository users;

    public List<UserTrainer> listMyTrainers(Long userId) {
        return links.findAllByUserId(userId);
    }

    public List<UserTrainer> listUsersForTrainer(Long trainerId) {
        return links.findAllByTrainerId(trainerId);
    }

    /**
     * Link user -> trainer.
     * Ako makePrimary=true: postavlja tog trenera kao primary (i gasi prethodnog primary ako postoji).
     */
    @Transactional
    public UserTrainer linkTrainer(Long userId, Long trainerId, boolean makePrimary) {

        // 1) Provjeri da trainer postoji i da je TRAINER
        User trainer = users.findById(trainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));
        if (trainer.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a TRAINER");
        }

        // 2) Ako link već postoji -> samo eventualno promijeni primary
        UserTrainer ut = links.findByUserIdAndTrainerId(userId, trainerId)
                .orElseGet(() -> {
                    UserTrainer created = new UserTrainer();
                    created.setUserId(userId);
                    created.setTrainerId(trainerId);
                    created.setPrimaryTrainer(false);
                    return created;
                });

        // 3) Ako treba primary, ugasi stari + upali novi
        if (makePrimary) {
            links.findByUserIdAndPrimaryTrainerTrue(userId).ifPresent(existing -> {
                if (!existing.getTrainerId().equals(trainerId)) {
                    existing.setPrimaryTrainer(false);
                    links.save(existing);
                }
            });
            ut.setPrimaryTrainer(true);
        }

        return links.save(ut);
    }

    @Transactional
    public void unlinkTrainer(Long userId, Long trainerId) {
        links.deleteByUserIdAndTrainerId(userId, trainerId);
    }

    @Transactional
    public void setPrimaryTrainer(Long userId, Long trainerId) {

        UserTrainer target = links.findByUserIdAndTrainerId(userId, trainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer link not found"));

        links.findByUserIdAndPrimaryTrainerTrue(userId).ifPresent(existing -> {
            if (!existing.getTrainerId().equals(trainerId)) {
                existing.setPrimaryTrainer(false);
                links.save(existing);
            }
        });

        target.setPrimaryTrainer(true);
        links.save(target);
    }

    public Long getPrimaryTrainerIdOrNull(Long userId) {
        return links.findByUserIdAndPrimaryTrainerTrue(userId)
                .map(UserTrainer::getTrainerId)
                .orElse(null);
    }

    /**
     * Za schedule: user smije koristiti samo trenera na kojeg je linkan.
     */
    public void assertUserLinkedToTrainer(Long userId, Long trainerId) {
        boolean exists = links.findByUserIdAndTrainerId(userId, trainerId).isPresent();
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not linked to selected trainer");
        }
    }
}
