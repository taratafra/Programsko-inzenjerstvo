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

    @Transactional
    public UserTrainer linkTrainer(Long userId, Long trainerId, boolean makePrimary) {
        // provjeri da trainer postoji i da je stvarno TRAINER
        User trainer = users.findById(trainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));

        if (trainer.getRole() != Role.TRAINER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a TRAINER");
        }

        UserTrainer ut = links.findByUserIdAndTrainerId(userId, trainerId)
                .orElseGet(() -> UserTrainer.builder()
                        .userId(userId)
                        .trainerId(trainerId)
                        .primaryTrainer(false)
                        .build()
                );

        ut = links.save(ut);

        if (makePrimary) {
            setPrimaryTrainer(userId, trainerId);
            ut.setPrimaryTrainer(true);
        }

        return ut;
    }

    @Transactional
    public void unlinkTrainer(Long userId, Long trainerId) {
        // ako briše primary, samo ga brišemo (user poslije mora postaviti novog)
        links.deleteByUserIdAndTrainerId(userId, trainerId);
    }

    @Transactional
    public void setPrimaryTrainer(Long userId, Long trainerId) {
        // mora postojati link
        UserTrainer target = links.findByUserIdAndTrainerId(userId, trainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer link not found"));

        // ugasi stari primary
        links.findByUserIdAndPrimaryTrainerTrue(userId).ifPresent(existing -> {
            if (!existing.getTrainerId().equals(trainerId)) {
                existing.setPrimaryTrainer(false);
                links.save(existing);
            }
        });

        // upali novi primary
        target.setPrimaryTrainer(true);
        links.save(target);
    }

    public Long getPrimaryTrainerIdOrNull(Long userId) {
        return links.findByUserIdAndPrimaryTrainerTrue(userId)
                .map(UserTrainer::getTrainerId)
                .orElse(null);
    }
}
