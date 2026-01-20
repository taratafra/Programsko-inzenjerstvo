package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.DailyFocusResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.BadgeAward;
import Pomna_Sedmica.Mindfulnes.domain.entity.DailyFocus;
import Pomna_Sedmica.Mindfulnes.domain.entity.DailyFocusAnswer;
import Pomna_Sedmica.Mindfulnes.domain.entity.UserStreak;
import Pomna_Sedmica.Mindfulnes.domain.enums.BadgeType;
import Pomna_Sedmica.Mindfulnes.repository.BadgeAwardRepository;
import Pomna_Sedmica.Mindfulnes.repository.DailyFocusAnswerRepository;
import Pomna_Sedmica.Mindfulnes.repository.DailyFocusRepository;
import Pomna_Sedmica.Mindfulnes.repository.UserStreakRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DailyFocusService {

    private final DailyFocusRepository focusRepo;
    private final DailyFocusAnswerRepository answerRepo;
    private final UserStreakRepository streakRepo;
    private final BadgeAwardRepository badgeRepo;
    private final MoodQuestionCatalogLoader catalog;

    public static final int DEFAULT_QUESTION_COUNT = 3;

    public DailyFocus getOrCreate(Long userId, LocalDate date) {
        LocalDate d = (date == null) ? LocalDate.now() : date;

        return focusRepo.findByUserIdAndDate(userId, d)
                .orElseGet(() -> {
                    DailyFocus f = DailyFocus.builder()
                            .userId(userId)
                            .date(d)
                            .selectedQuestionIds(pickDailyQuestions(userId, d, DEFAULT_QUESTION_COUNT))
                            .completed(false)
                            .build();
                    return focusRepo.save(f);
                });
    }

    public DailyFocusResponse getResponse(Long userId, LocalDate date) {
        DailyFocus f = getOrCreate(userId, date);
        List<DailyFocusResponse.QuestionItem> items = resolveQuestionTexts(f.getSelectedQuestionIds());
        return DailyFocusResponse.from(f, items);
    }

    /**
     * Complete fokus:
     * - sprema odgovore (ako postoje)
     * - označi daily focus kao completed
     * - update streak
     * - award badges
     */
    public DailyFocusResponse complete(Long userId, LocalDate date, Map<String, String> answers) {
        LocalDate d = (date == null) ? LocalDate.now() : date;

        DailyFocus f = getOrCreate(userId, d);

        // 1) spremi answers (upsert), ali samo za pitanja koja su dana taj dan
        if (answers != null && !answers.isEmpty()) {
            Set<String> allowed = new HashSet<>(f.getSelectedQuestionIds());
            for (var e : answers.entrySet()) {
                String qid = e.getKey();
                String text = e.getValue();

                if (qid == null || qid.isBlank()) continue;
                if (text == null) text = "";
                text = text.trim();
                if (text.isEmpty()) continue;

                // opcionalno: samo ako je u današnjem setu pitanja
                if (!allowed.contains(qid)) continue;

                DailyFocusAnswer a = answerRepo.findByUserIdAndDateAndQuestionId(userId, d, qid)
                        .orElseGet(() -> DailyFocusAnswer.builder()
                                .userId(userId)
                                .date(d)
                                .questionId(qid)
                                .build());

                a.setAnswerText(text);
                answerRepo.save(a);
            }
        }

        // 2) completion + streak/badges (idempotentno)
        if (!Boolean.TRUE.equals(f.getCompleted())) {
            f.setCompleted(true);
            f.setCompletedAt(Instant.now());
            focusRepo.save(f);

            UserStreak streak = updateStreak(userId, d);
            awardBadgesIfNeeded(userId, streak.getCurrentStreak());
        }

        return DailyFocusResponse.from(f, resolveQuestionTexts(f.getSelectedQuestionIds()));
    }

    public UserStreak getOrCreateStreak(Long userId) {
        return streakRepo.findByUserId(userId)
                .orElseGet(() -> streakRepo.save(UserStreak.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .longestStreak(0)
                        .build()));
    }

    /**
     * Streak logika:
     * - ako je zadnji completion danas -> ne mijenjaj
     * - ako je zadnji completion jučer -> +1
     * - ako je gap >= 1 dan -> RESET na 0 pa današnji completion kreće kao 1
     */
    private UserStreak updateStreak(Long userId, LocalDate completedDate) {
        UserStreak s = getOrCreateStreak(userId);

        LocalDate last = s.getLastCompletedDate();

        if (last == null) {
            // prvi put ikad
            s.setCurrentStreak(1);

        } else if (last.equals(completedDate)) {
            // isti dan -> ništa (idempotentno)
            // ne diramo currentStreak

        } else if (last.plusDays(1).equals(completedDate)) {
            // nastavak niza (jučer -> danas)
            s.setCurrentStreak(s.getCurrentStreak() + 1);

        } else {
            // prekid niza: reset na 0, pa današnji completion kreće novi niz (1)
            s.setCurrentStreak(0);
            s.setCurrentStreak(1);
        }

        s.setLastCompletedDate(completedDate);

        if (s.getCurrentStreak() > s.getLongestStreak()) {
            s.setLongestStreak(s.getCurrentStreak());
        }

        return streakRepo.save(s);
    }

    private void awardBadgesIfNeeded(Long userId, int currentStreak) {
        // NEW: čim korisnik prvi put ikad završi DailyFocus, streak postaje 1
        if (currentStreak >= 1) awardOnce(userId, BadgeType.FIRST_SUBMISSION);

        if (currentStreak >= 7) awardOnce(userId, BadgeType.STREAK_7);
        if (currentStreak >= 30) awardOnce(userId, BadgeType.STREAK_30);
        if (currentStreak >= 100) awardOnce(userId, BadgeType.STREAK_100);
    }


    private void awardOnce(Long userId, BadgeType type) {
        if (badgeRepo.existsByUserIdAndBadgeType(userId, type)) return;
        badgeRepo.save(BadgeAward.builder()
                .userId(userId)
                .badgeType(type)
                .awardedAt(Instant.now())
                .build());
    }

    private List<String> pickDailyQuestions(Long userId, LocalDate date, int count) {
        JsonNode questions = catalog.root().path("questions");
        if (!questions.isArray() || questions.size() == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No questions in catalog");
        }

        Random rng = new Random(Objects.hash(userId, date));

        List<String> ids = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) indexes.add(i);
        Collections.shuffle(indexes, rng);

        int take = Math.min(count, indexes.size());
        for (int i = 0; i < take; i++) {
            JsonNode q = questions.get(indexes.get(i));
            ids.add(q.path("id").asText());
        }
        return ids;
    }

    private List<DailyFocusResponse.QuestionItem> resolveQuestionTexts(List<String> ids) {
        JsonNode questions = catalog.root().path("questions");

        Map<String, String> map = new HashMap<>();
        if (questions.isArray()) {
            for (JsonNode q : questions) {
                map.put(q.path("id").asText(), q.path("text").asText());
            }
        }

        List<DailyFocusResponse.QuestionItem> out = new ArrayList<>();
        for (String id : ids) {
            out.add(new DailyFocusResponse.QuestionItem(id, map.getOrDefault(id, "")));
        }
        return out;
    }
}
