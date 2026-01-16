package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.MoodCheckInRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.MoodCheckIn;
import Pomna_Sedmica.Mindfulnes.repository.MoodCheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoodCheckInService {

    private final MoodCheckInRepository repo;

    public MoodCheckIn upsertForUser(Long userId, MoodCheckInRequest req) {
        LocalDate date = (req.date() == null) ? LocalDate.now() : req.date();

        MoodCheckIn e = repo.findByUserIdAndDate(userId, date)
                .orElseGet(() -> MoodCheckIn.builder()
                        .userId(userId)
                        .date(date)
                        .build());

        e.setMoodScore(req.moodScore());
        e.setEmotions(req.emotions() != null ? req.emotions() : e.getEmotions());
        e.setSleepQuality(req.sleepQuality());
        e.setStressLevel(req.stressLevel());
        e.setFocusLevel(req.focusLevel());
        e.setCaffeineIntake(req.caffeineIntake());
        e.setAlcoholIntake(req.alcoholIntake());
        e.setPhysicalActivity(req.physicalActivity());
        e.setNotes(req.notes());

        return repo.save(e);
    }

    public MoodCheckIn getForUserAndDate(Long userId, LocalDate date) {
        return repo.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in not found"));
    }

    public List<MoodCheckIn> listForUser(Long userId, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return repo.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
        }
        return repo.findAllByUserIdOrderByDateDesc(userId);
    }
}
