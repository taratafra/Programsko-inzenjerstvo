package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.PracticePlanResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.OnboardingSurvey;
import Pomna_Sedmica.Mindfulnes.domain.entity.PracticePlan;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.repository.OnboardingSurveyRepository;
import Pomna_Sedmica.Mindfulnes.repository.PracticePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticePlanService {

    private final PracticePlanRepository plans;
    private final OnboardingSurveyRepository surveys;
    private final PracticePlanCatalogLoader catalog;

    private final ObjectMapper mapper = new ObjectMapper();

    public PracticePlanResponse getOrCreateForUser(Long userId, boolean forceRefresh) {
        LocalDate today = LocalDate.now();
        LocalDate to = today.plusDays(6);

        if (!forceRefresh) {
            var existing = plans.findFirstByUserIdAndValidFromLessThanEqualAndValidToGreaterThanEqualOrderByValidFromDesc(
                    userId, today, today
            );
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        OnboardingSurvey survey = surveys.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Onboarding survey missing for user"));

        PracticePlan created = generateAndStore(userId, survey, today, to);
        return toResponse(created);
    }

    private PracticePlan generateAndStore(Long userId, OnboardingSurvey s, LocalDate from, LocalDate to) {
        JsonNode templates = catalog.root().path("templates");
        if (!templates.isArray() || templates.size() == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No plan templates available");
        }

        Random rng = new Random(Objects.hash(userId, from));

        JsonNode chosen = chooseTemplate(templates, s, rng);

        String key = chosen.path("key").asText("UNKNOWN");
        JsonNode days = chosen.path("days");
        if (!days.isArray() || days.size() < 7) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Plan template must have 7 days: " + key);
        }

        // map days -> attach real dates (today..today+6)
        List<Map<String, Object>> outDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            JsonNode d = days.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", from.plusDays(i).toString());
            item.put("title", d.path("title").asText(""));
            item.put("description", d.path("description").asText(""));
            if (d.hasNonNull("estimatedMinutes")) item.put("estimatedMinutes", d.get("estimatedMinutes").asInt());
            outDays.add(item);
        }

        String planJson;
        try {
            planJson = mapper.writeValueAsString(outDays);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize plan JSON", e);
        }

        PracticePlan plan = PracticePlan.builder()
                .userId(userId)
                .validFrom(from)
                .validTo(to)
                .generatedAt(Instant.now())
                .templateKey(key)
                .planJson(planJson)
                .build();

        return plans.save(plan);
    }

    private JsonNode chooseTemplate(JsonNode templates, OnboardingSurvey s, Random rng) {
        List<JsonNode> matches = new ArrayList<>();
        JsonNode fallback = null;

        for (JsonNode t : templates) {
            JsonNode cond = t.path("conditions");
            if (cond.path("fallback").asBoolean(false)) {
                fallback = t;
                continue;
            }
            if (matchesConditions(cond, s)) {
                matches.add(t);
            }
        }

        if (!matches.isEmpty()) {
            return matches.get(rng.nextInt(matches.size()));
        }
        if (fallback != null) return fallback;

        // last resort
        return templates.get(0);
    }

    private boolean matchesConditions(JsonNode cond, OnboardingSurvey s) {
        // stress/sleep are ints 1..5 (from your entity)
        int stress = s.getStressLevel();
        int sleep = s.getSleepQuality();

        if (cond.has("minStressLevel") && stress < cond.get("minStressLevel").asInt()) return false;
        if (cond.has("maxStressLevel") && stress > cond.get("maxStressLevel").asInt()) return false;

        // "minSleepQualityMax": "sleepQuality <= X" (npr. loš san)
        if (cond.has("minSleepQualityMax") && sleep > cond.get("minSleepQualityMax").asInt()) return false;
        if (cond.has("minSleepQualityMin") && sleep < cond.get("minSleepQualityMin").asInt()) return false;

        if (cond.has("experience")) {
            String exp = s.getMeditationExperience().name();
            boolean ok = false;
            for (JsonNode e : cond.get("experience")) {
                if (exp.equalsIgnoreCase(e.asText())) { ok = true; break; }
            }
            if (!ok) return false;
        }

        if (cond.has("goalsAny")) {
            Set<String> userGoals = new HashSet<>();
            if (s.getGoals() != null) {
                for (Goal g : s.getGoals()) userGoals.add(g.name());
            }
            boolean ok = false;
            for (JsonNode g : cond.get("goalsAny")) {
                if (userGoals.contains(g.asText())) { ok = true; break; }
            }
            if (!ok) return false;
        }

        return true;
    }

    private PracticePlanResponse toResponse(PracticePlan plan) {
        try {
            JsonNode arr = mapper.readTree(plan.getPlanJson());
            List<PracticePlanResponse.DayItem> days = new ArrayList<>();
            for (JsonNode n : arr) {
                LocalDate date = LocalDate.parse(n.path("date").asText());
                String title = n.path("title").asText("");
                String desc = n.path("description").asText("");
                Integer mins = n.hasNonNull("estimatedMinutes") ? n.get("estimatedMinutes").asInt() : null;
                days.add(new PracticePlanResponse.DayItem(date, title, desc, mins));
            }

            return new PracticePlanResponse(
                    plan.getId(),
                    plan.getUserId(),
                    plan.getValidFrom(),
                    plan.getValidTo(),
                    plan.getTemplateKey(),
                    days
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse stored plan JSON", e);
        }
    }
}
