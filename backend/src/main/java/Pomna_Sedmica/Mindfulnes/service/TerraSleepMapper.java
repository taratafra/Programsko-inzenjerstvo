package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SleepScoreWebhookRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Maps Terra webhook payloads to our internal SleepScoreWebhookRequest.
 *
 * The schema can vary slightly by provider, so this mapper is defensive and
 * attempts to extract the most relevant fields.
 */
@Service
public class TerraSleepMapper {

    public List<SleepScoreWebhookRequest> toSleepScoreWebhookRequests(JsonNode event) {
        List<SleepScoreWebhookRequest> out = new ArrayList<>();
        if (event == null || event.isNull()) return out;

        String type = text(event.get("type"));
        if (type == null || !type.toLowerCase().contains("sleep")) {
            return out;
        }

        JsonNode userNode = event.get("user");
        String terraUserId = text(userNode, "user_id");
        if (terraUserId == null || terraUserId.isBlank()) {
            // Some payloads might place user_id at top-level.
            terraUserId = text(event, "user_id");
        }

        JsonNode data = event.get("data");
        if (data == null || !data.isArray()) return out;

        for (JsonNode session : data) {
            Integer score = extractSleepScore(session);
            if (score == null) continue;

            LocalDate date = extractSessionDate(session);
            Integer latencyMin = extractLatencyMinutes(session);
            Integer awakenings = extractAwakenings(session);
            Integer continuity = extractContinuity(session);

            out.add(new SleepScoreWebhookRequest(
                    terraUserId,
                    date,
                    score,
                    latencyMin,
                    awakenings,
                    continuity,
                    session.toString()
            ));
        }

        return out;
    }

    private Integer extractSleepScore(JsonNode session) {
        // Terra provides a unified score under scores.sleep, and some providers also supply enrichment.
        Integer fromEnrichment = intOrNull(session, "data_enrichment", "sleep_score");
        if (fromEnrichment != null) return clamp0to100(fromEnrichment);
        Integer fromScores = intOrNull(session, "scores", "sleep");
        if (fromScores != null) return clamp0to100(fromScores);
        return null;
    }

    private Integer extractContinuity(JsonNode session) {
        // Map continuity to sleep efficiency if present.
        Integer eff = intOrNull(session, "sleep_durations_data", "sleep_efficiency");
        if (eff != null) return clamp0to100(eff);
        // Some providers return float efficiency; try double.
        Double effD = doubleOrNull(session, "sleep_durations_data", "sleep_efficiency");
        if (effD != null) return clamp0to100((int) Math.round(effD));
        return null;
    }

    private Integer extractLatencyMinutes(JsonNode session) {
        // Try explicit latency fields first.
        Integer latencySeconds = findIntByName(session, "sleep_latency_seconds");
        if (latencySeconds == null) latencySeconds = findIntByName(session, "latency_seconds");
        if (latencySeconds != null) return (int) Math.round(latencySeconds / 60.0);

        // Fallback: derive from hypnogram samples (first non-awake sample).
        JsonNode metaStart = session.path("metadata").get("start_time");
        OffsetDateTime start = parseDateTime(metaStart);
        if (start == null) return null;

        JsonNode samples = session.path("sleep_durations_data").get("hypnogram_samples");
        if (samples == null || !samples.isArray()) return null;

        for (JsonNode s : samples) {
            String level = text(s, "level");
            OffsetDateTime ts = parseDateTime(s.get("timestamp"));
            if (ts == null || level == null) continue;
            if (!isAwakeLevel(level)) {
                long minutes = Math.round((ts.toInstant().toEpochMilli() - start.toInstant().toEpochMilli()) / 60000.0);
                return (int) Math.max(0, minutes);
            }
        }
        return null;
    }

    private Integer extractAwakenings(JsonNode session) {
        // If provider gives an explicit count, use it.
        Integer explicit = findIntByName(session, "num_awakenings");
        if (explicit != null) return Math.max(0, explicit);

        // Fallback: count awake segments after sleep onset using hypnogram.
        JsonNode samples = session.path("sleep_durations_data").get("hypnogram_samples");
        if (samples == null || !samples.isArray()) return null;

        boolean startedSleeping = false;
        boolean inAwake = false;
        int awakenings = 0;

        for (JsonNode s : samples) {
            String level = text(s, "level");
            if (level == null) continue;

            if (!startedSleeping) {
                if (!isAwakeLevel(level)) {
                    startedSleeping = true;
                    inAwake = false;
                }
                continue;
            }

            if (isAwakeLevel(level)) {
                if (!inAwake) {
                    awakenings++;
                    inAwake = true;
                }
            } else {
                inAwake = false;
            }
        }

        return awakenings;
    }

    private LocalDate extractSessionDate(JsonNode session) {
        OffsetDateTime end = parseDateTime(session.path("metadata").get("end_time"));
        if (end == null) end = parseDateTime(session.path("metadata").get("start_time"));
        if (end == null) end = parseDateTime(session.get("end_time"));
        if (end == null) end = parseDateTime(session.get("start_time"));
        if (end == null) return null;
        return end.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private boolean isAwakeLevel(String level) {
        String l = level.toLowerCase();
        return l.contains("awake") || l.equals("wake");
    }

    private Integer clamp0to100(Integer v) {
        if (v == null) return null;
        return Math.max(0, Math.min(100, v));
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.asText(null);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        return text(node.get(field));
    }

    private Integer intOrNull(JsonNode node, String... path) {
        JsonNode n = node;
        for (String p : path) {
            if (n == null) return null;
            n = n.get(p);
        }
        if (n == null || n.isNull()) return null;
        if (n.isInt() || n.isLong()) return n.asInt();
        if (n.isTextual()) {
            try {
                return Integer.parseInt(n.asText());
            } catch (NumberFormatException ignored) {}
        }
        if (n.isDouble() || n.isFloat() || n.isBigDecimal()) {
            return (int) Math.round(n.asDouble());
        }
        return null;
    }

    private Double doubleOrNull(JsonNode node, String... path) {
        JsonNode n = node;
        for (String p : path) {
            if (n == null) return null;
            n = n.get(p);
        }
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.asDouble();
        if (n.isTextual()) {
            try {
                return Double.parseDouble(n.asText());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Searches the JSON tree for an integer field with the given name.
     * Defensive because provider-specific payloads sometimes nest these values.
     */
    private Integer findIntByName(JsonNode root, String fieldName) {
        if (root == null) return null;
        if (root.has(fieldName)) {
            JsonNode n = root.get(fieldName);
            if (n != null && !n.isNull()) {
                if (n.isNumber()) return n.asInt();
                if (n.isTextual()) {
                    try {
                        return Integer.parseInt(n.asText());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (root.isObject()) {
            Iterator<String> it = root.fieldNames();
            while (it.hasNext()) {
                String f = it.next();
                Integer v = findIntByName(root.get(f), fieldName);
                if (v != null) return v;
            }
        } else if (root.isArray()) {
            for (JsonNode c : root) {
                Integer v = findIntByName(c, fieldName);
                if (v != null) return v;
            }
        }
        return null;
    }

    private OffsetDateTime parseDateTime(JsonNode node) {
        String s = text(node);
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }
}
