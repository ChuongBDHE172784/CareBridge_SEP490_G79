package com.carebridge.backend.triage.policy;

import com.carebridge.backend.triage.entity.IntakeSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * CB-TRIAGE-THMC-IMP-001 §8.1 — minimization policy (BR-THMC-003 / TDS §17 C2):
 * summary_text and memory_payload_jsonb are built from STRUCTURED session data only
 * (stage, riskLevel, normalizedSymptoms from the canonical result snapshot,
 * recommendationCode, completion date). MUST NOT include parentFreeText, raw symptoms
 * text, or any conversation content (HealthMemoryEntry.java:32 oracle).
 *
 * <p>Whitelist extraction: only the {@code normalizedSymptoms}, {@code recommendationCode}
 * and {@code fallbackUsed} keys of the canonical result snapshot are ever read;
 * free-text keys ({@code parentFreeText}, {@code symptoms}, conversation content) are
 * never touched. Summary content is never logged (PDPA log hygiene, TDS §14.2).
 *
 * @version 1.0
 */
@Component
public class HealthMemorySummaryPolicy {

    static final String PAYLOAD_SCHEMA_VERSION = "1.0";

    /** Bounded defensive caps so a hostile snapshot can never bloat a memory row. */
    private static final int MAX_SYMPTOMS = 12;
    private static final int MAX_SYMPTOM_CHARS = 60;
    /** Machine-readable symptom codes only — anything else is not "normalized" data. */
    private static final String SAFE_TOKEN_PATTERN = "[\\p{L}0-9_\\- ]{1,60}";

    private final ObjectMapper objectMapper;

    public HealthMemorySummaryPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HealthMemorySummaryPolicy() {
        this(new ObjectMapper());
    }

    public String buildSummary(IntakeSession session) {
        StringBuilder summary = new StringBuilder();
        summary.append(stageName(session)).append(" triage");
        if (session.getCompletedAt() != null) {
            summary.append(" on ").append(LocalDate.ofInstant(session.getCompletedAt(), ZoneOffset.UTC));
        }
        summary.append(": risk ").append(session.getRiskLevel() == null
                ? "UNKNOWN" : session.getRiskLevel().name());
        List<String> symptoms = normalizedSymptoms(session);
        if (!symptoms.isEmpty()) {
            summary.append("; symptoms ").append(String.join(", ", symptoms));
        }
        String recommendationCode = recommendationCode(session);
        if (recommendationCode != null) {
            summary.append("; advice ").append(recommendationCode);
        }
        summary.append('.');
        return summary.toString();
    }

    /** Builds memory_payload_jsonb (schemaVersion "1.0" — TDS §5.2) under the same minimization rule. */
    public String buildPayloadJson(IntakeSession session) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schemaVersion", PAYLOAD_SCHEMA_VERSION);
        payload.put("sourceSessionId", session.getId() == null ? null : session.getId().toString());
        payload.put("stage", stageName(session));
        payload.put("riskLevel", session.getRiskLevel() == null ? null : session.getRiskLevel().name());
        payload.put("recommendationCode", recommendationCode(session));
        ArrayNode symptoms = payload.putArray("normalizedSymptoms");
        normalizedSymptoms(session).forEach(symptoms::add);
        payload.put("fallbackUsed", resultSnapshot(session).path("fallbackUsed").asBoolean(false));
        payload.put("completedAt", session.getCompletedAt() == null
                ? null : session.getCompletedAt().toString());
        return payload.toString();
    }

    private String stageName(IntakeSession session) {
        // Legacy pre-stage sessions are pediatric (TriageService.sessionStage precedent)
        return session.getStage() == null ? "INFANT" : session.getStage().name();
    }

    /**
     * Canonical result snapshot: resultJson when populated by applyCanonicalSnapshot,
     * else the raw response; conversation envelopes nest the result under triageResult.
     */
    private JsonNode resultSnapshot(IntakeSession session) {
        String json = session.getResultJson() != null && !session.getResultJson().isBlank()
                && !"{}".equals(session.getResultJson().trim())
                ? session.getResultJson()
                : session.getRawAiResponse();
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode nested = root.path("triageResult");
            return nested.isObject() ? nested : root;
        } catch (Exception exception) {
            // Malformed snapshot: fall back to structured entity fields only (no free text)
            return objectMapper.createObjectNode();
        }
    }

    private List<String> normalizedSymptoms(IntakeSession session) {
        JsonNode node = resultSnapshot(session).path("normalizedSymptoms");
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank() && value.length() <= MAX_SYMPTOM_CHARS
                    && value.matches(SAFE_TOKEN_PATTERN)) {
                values.add(value);
            }
            if (values.size() >= MAX_SYMPTOMS) {
                break;
            }
        }
        return values;
    }

    private String recommendationCode(IntakeSession session) {
        String value = resultSnapshot(session).path("recommendationCode").asText(null);
        return value == null || value.isBlank() || !value.matches("[A-Z0-9_]{1,60}") ? null : value;
    }
}
