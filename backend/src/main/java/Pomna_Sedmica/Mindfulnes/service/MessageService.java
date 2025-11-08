package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.controller.dto.MessageItem;
import Pomna_Sedmica.Mindfulnes.controller.dto.MessageResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.domain.enums.TimeOfDay;
import Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final OnboardingSurveyRepository surveys;
    private final MessageCatalogLoader catalog;

    /**
     * Sastavi poruke za korisnika:
     * - userId: citamo OnboardingSurvey
     * - tod: MORNING/MIDDAY/EVENING (obavezno vec odredeno u kontroleru)
     * - count: broj poruka (1..3 tipicno)
     * - seed: stabilizacija (ako zelimo da bude isto tijekom dana)
     */
    public Optional<MessageResponse> composeForUser(Long userId, TimeOfDay tod, Integer count, String seed) {
        var surveyOpt = surveys.findByUserId(userId);
        if (surveyOpt.isEmpty()) return Optional.empty();

        OnboardingSurvey s = surveyOpt.get();
        JsonNode root = catalog.root();

        Random rng = (seed == null || seed.isBlank())
                ? ThreadLocalRandom.current()
                : new Random(seed.hashCode());

        String stressBucket = resolveBucket(root.path("buckets").path("stress"), s.getStressLevel(), Map.of(
                "LOW", List.of(1,2), "MEDIUM", List.of(3), "HIGH", List.of(4,5)
        ));
        String sleepBucket  = resolveBucket(root.path("buckets").path("sleep"), s.getSleepQuality(), Map.of(
                "POOR", List.of(1,2), "OK", List.of(3), "GOOD", List.of(4,5)
        ));

        List<String> goals = new ArrayList<>();
        s.getGoals().forEach(g -> goals.add(g.name())); // npr. "IMPROVE_SLEEP"
        Collections.shuffle(goals, rng);
        int desired = (count == null || count < 1) ? 2 : Math.min(count, Math.max(1, goals.size()));

        List<MessageItem> out = new ArrayList<>();
        for (int i = 0; i < desired && i < goals.size(); i++) {
            String goal = goals.get(i);

            String base = pickOne(root.path("baseByGoalAndTime").path(goal).path(tod.name()), rng);
            String stressAddon = pickOne(root.path("stressAddons").path(stressBucket), rng);
            String sleepAddon  = pickOne(root.path("sleepAddons").path(sleepBucket), rng);
            String expAddon    = pickOne(root.path("experienceAddons").path(s.getMeditationExperience().name()), rng);
            String closing     = pickOne(root.path("closing"), rng);

            String text = String.join(" ", filterNonEmpty(base, stressAddon, sleepAddon, expAddon, closing));
            out.add(new MessageItem(goal, text));
        }

        if (out.isEmpty()) {
            JsonNode baseByGoals = root.path("baseByGoalAndTime");
            Iterator<String> fieldNames = baseByGoals.fieldNames();
            while (fieldNames.hasNext()) {
                String g = fieldNames.next();
                JsonNode arr = baseByGoals.path(g).path(tod.name());
                if (arr.isArray() && arr.size() > 0) {
                    String base = pickOne(arr, rng);
                    String closing = pickOne(root.path("closing"), rng);
                    out.add(new MessageItem(g, String.join(" ", filterNonEmpty(base, closing))));
                    break;
                }
            }
        }

        return Optional.of(new MessageResponse(tod, out));
    }

    private String resolveBucket(JsonNode bucketNode, int value, Map<String, List<Integer>> fallback) {
        if (bucketNode != null && bucketNode.isObject() && bucketNode.size() > 0) {
            Iterator<String> it = bucketNode.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                for (JsonNode n : bucketNode.get(key)) {
                    if (n.asInt() == value) return key;
                }
            }
        }
        // fallback logika
        for (var e : fallback.entrySet()) {
            if (e.getValue().contains(value)) return e.getKey();
        }
        // default
        return fallback.keySet().iterator().next();
    }

    private String pickOne(JsonNode arr, Random rng) {
        if (arr == null || !arr.isArray() || arr.size() == 0) return "";
        int idx = rng.nextInt(arr.size());
        return arr.get(idx).asText();
    }

    private List<String> filterNonEmpty(String... parts) {
        List<String> res = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) res.add(p.trim());
        }
        return res;
    }

    public static String defaultDailySeed(Long userId, TimeOfDay tod) {
        return userId + "_" + LocalDate.now() + "_" + tod.name();
    }
}
