package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.TriageRecommendationCode;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stable read model for triage history, result and handoff consumers.
 *
 * <p>All mutations are owned by {@link CanonicalTriageSessionService}. This class deliberately
 * contains no rule, provider, fallback or conversation logic. It only projects both canonical
 * session envelopes and rows created before the cutover onto the long-lived public read DTOs.
 */
@Service
@Transactional(readOnly = true)
public class TriageService implements ITriageService {
    private final IIntakeSessionRepository repository;
    private final EvidenceSourceService evidenceSourceService;
    private final ObjectMapper objectMapper;

    public TriageService(
            IIntakeSessionRepository repository,
            EvidenceSourceService evidenceSourceService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.evidenceSourceService = evidenceSourceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TriageResultResponse getResult(UUID sessionId, UUID userId) {
        IntakeSession session = repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-003",
                        "Intake session not found: " + sessionId));
        List<Map<String, Object>> rawCitations = readObjectList(session, "citations");
        List<Map<String, Object>> citations = readValidatedCitations(session);
        String warning = readText(session, "warning");
        if (!rawCitations.isEmpty() && citations.isEmpty()) {
            warning = "Nguồn bằng chứng không hợp lệ đã bị loại; rủi ro vẫn do bộ quy tắc quyết định.";
        }
        boolean exposeContinuation = hasActiveTerminalContinuation(session);
        return TriageResultResponse.builder()
                .sessionId(session.getId())
                .stage(sessionStageName(session))
                .triageStatus(readText(session, "status", session.getStatus().name()))
                .riskLevel(readText(session, "riskLevel"))
                .riskColor(readText(session, "riskColor"))
                .summary(readText(session, "summary"))
                .possibleConcern(readText(session, "possibleConcern"))
                .recommendedAction(readText(session, "recommendedAction"))
                .emergencyActionRequired(readBoolean(session, "emergencyActionRequired"))
                .redFlags(readStringList(session, "redFlags"))
                .matchedRules(readStringList(session, "matchedRules"))
                .citations(citations)
                .ragAnswer(readText(session, "ragAnswer"))
                .ragDisclaimer(readText(session, "ragDisclaimer"))
                .ragFallback(readNullableBoolean(session, "ragFallback"))
                .claims(readValidatedClaims(session, citations))
                .evidence(readObject(session, "evidence"))
                .disclaimer(readText(session, "disclaimer", session.getDisclaimer()))
                .questions(readStringList(session, "questions"))
                .warning(warning)
                .normalizedSymptoms(readStringList(session, "normalizedSymptoms"))
                .normalizedSymptomDetails(readObjectList(session, "normalizedSymptomDetails"))
                .evidenceIds(readStringList(session, "evidenceIds"))
                .recommendationCode(readText(session, "recommendationCode"))
                .explainabilityMetrics(readObject(session, "explainabilityMetrics"))
                .graphVersion(readText(session, "graphVersion"))
                .ruleSetVersion(readText(session, "ruleSetVersion"))
                .ontologyVersion(readText(session, "ontologyVersion"))
                .responseSchemaVersion(readText(session, "responseSchemaVersion", "1.0"))
                .fallbackUsed(readBoolean(session, "fallbackUsed"))
                .status(session.getStatus().name())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .journeyId(session.getJourneyId())
                .originDashboard(session.getOriginDashboard() == null ? null : session.getOriginDashboard().name())
                .originReferenceId(session.getOriginReferenceId())
                .continuationToken(exposeContinuation ? session.getContinuationToken() : null)
                .continuationExpiresAt(exposeContinuation ? session.getContinuationExpiresAt() : null)
                .build();
    }

    @Override
    public List<IntakeSessionResponse> listSessions(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    private IntakeSessionResponse toResponse(IntakeSession session) {
        boolean exposeContinuation = hasActiveTerminalContinuation(session);
        return IntakeSessionResponse.builder()
                .sessionId(session.getId()).stage(sessionStageName(session))
                .status(session.getStatus().name())
                .riskLevel(session.getRiskLevel() == null ? null : session.getRiskLevel().name())
                .disclaimer(session.getDisclaimer()).createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt()).journeyId(session.getJourneyId())
                .originDashboard(session.getOriginDashboard() == null ? null : session.getOriginDashboard().name())
                .originReferenceId(session.getOriginReferenceId())
                .continuationToken(exposeContinuation ? session.getContinuationToken() : null)
                .continuationExpiresAt(exposeContinuation ? session.getContinuationExpiresAt() : null)
                .build();
    }

    private boolean hasActiveTerminalContinuation(IntakeSession session) {
        return session.getStatus() == IntakeStatus.COMPLETED && session.getRiskLevel() != null
                && session.getContinuationToken() != null && session.getContinuationAcknowledgedAt() == null
                && session.getContinuationExpiresAt() != null
                && session.getContinuationExpiresAt().isAfter(Instant.now());
    }

    private String sessionStageName(IntakeSession session) {
        if (isCanonical(session)) {
            String canonical = readText(session, "stage");
            if (canonical != null) return switch (canonical) {
                case "POSTPARTUM_MOTHER" -> TriageStage.POSTPARTUM.name();
                case "INFANT_0_12M" -> TriageStage.INFANT.name();
                case "TODDLER_12_24M" -> TriageStage.TODDLER.name();
                default -> canonical;
            };
        }
        return (session.getStage() == null ? TriageStage.INFANT : session.getStage()).name();
    }

    private boolean isCanonical(IntakeSession session) {
        return session.getSchemaVersion() != null && session.getSchemaVersion().startsWith("triage-v2-");
    }

    private JsonNode rawResult(IntakeSession session) {
        if (isCanonical(session)) return canonicalSessionResult(session);
        if (session.getRawAiResponse() == null || session.getRawAiResponse().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(session.getRawAiResponse());
            JsonNode nested = root.path("triageResult");
            return nested.isObject() ? nested : root;
        } catch (JsonProcessingException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode canonicalSessionResult(IntakeSession session) {
        if (session.getResultJson() == null || session.getResultJson().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode envelope = objectMapper.readTree(session.getResultJson());
            JsonNode state = envelope.path("triageState");
            if (state.isMissingNode() || state.isNull()) {
                // Compatibility reader for sessions persisted before the single-engine cutover.
                state = envelope.path("v2State");
            }
            JsonNode response = envelope.path("publicResponse");
            if (!state.isObject() || !response.isObject()) return objectMapper.createObjectNode();
            ObjectNode projection = objectMapper.createObjectNode();
            copyText(response, "stage", projection, "stage");
            copyText(response, "outcome", projection, "riskLevel");
            copyText(response, "action", projection, "recommendedAction");
            copyText(state, "finalResponse", projection, "summary");
            copyText(state, "rulesetVersion", projection, "ruleSetVersion");
            copyNode(state, "decisiveRuleIds", projection, "matchedRules");
            copyNode(response, "citations", projection, "citations");
            copyNode(response, "questions", projection, "questions");
            String risk = projection.path("riskLevel").asText(null);
            projection.put("emergencyActionRequired", "RED".equals(risk));
            projection.put("recommendationCode", TriageRecommendationCode.forRisk(risk));
            projection.put("responseSchemaVersion", session.getSchemaVersion());
            projection.put("graphVersion", "canonical-deterministic");
            projection.put("fallbackUsed", "FALLBACK_ONLY".equals(
                    response.path("readiness").path("technicalStatus").asText()));
            projection.put("status", session.getStatus().name());
            return projection;
        } catch (JsonProcessingException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static void copyText(JsonNode source, String sourceField, ObjectNode target, String targetField) {
        JsonNode value = source.path(sourceField);
        if (value.isTextual() && !value.asText().isBlank()) target.put(targetField, value.asText());
    }

    private static void copyNode(JsonNode source, String sourceField, ObjectNode target, String targetField) {
        JsonNode value = source.path(sourceField);
        if (!value.isMissingNode() && !value.isNull()) target.set(targetField, value.deepCopy());
    }

    private String readText(IntakeSession session, String field) { return readText(session, field, null); }

    private String readText(IntakeSession session, String field, String fallback) {
        JsonNode node = rawResult(session).path(field);
        if (node.isMissingNode() || node.isNull()) {
            if ("riskLevel".equals(field) && session.getRiskLevel() != null) return session.getRiskLevel().name();
            return fallback;
        }
        return node.asText();
    }

    private Boolean readBoolean(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        return node.isMissingNode() || node.isNull() ? Boolean.FALSE : node.asBoolean();
    }

    private Boolean readNullableBoolean(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asBoolean();
    }

    private List<String> readStringList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private List<Map<String, Object>> readObjectList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) return Collections.emptyList();
        List<Map<String, Object>> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isObject()) values.add(objectMapper.convertValue(item, new TypeReference<>() {}));
        });
        return values;
    }

    private Map<String, Object> readObject(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        return node.isObject() ? objectMapper.convertValue(node, new TypeReference<>() {}) : Collections.emptyMap();
    }

    private List<Map<String, Object>> readValidatedCitations(IntakeSession session) {
        List<Map<String, Object>> valid = new ArrayList<>();
        boolean legacy = "1.0".equals(readText(session, "responseSchemaVersion", "1.0"));
        for (Map<String, Object> citation : readObjectList(session, "citations")) {
            String title = nonBlank(citation.get("title"));
            String organization = nonBlank(citation.get("organization"));
            if (organization == null) organization = nonBlank(citation.get("source"));
            String url = nonBlank(citation.get("url"));
            if (isCanonical(session)) {
                if (title == null || organization == null || url == null
                        || nonBlank(citation.get("sourceId")) == null
                        || nonBlank(citation.get("section")) == null
                        || !"SOURCE_VERIFIED".equals(citation.get("sourceStatus"))
                        || !"LOCAL_BM25".equals(citation.get("retrievalMode"))
                        || !isOfficialHttpsUrl(url)) continue;
                valid.add(new LinkedHashMap<>(citation));
                continue;
            }
            String excerpt = nonBlank(citation.get("excerpt"));
            if (title == null || organization == null || url == null || excerpt == null
                    || !isOfficialHttpsUrl(url)) continue;
            Map<String, Object> safe = new LinkedHashMap<>(citation);
            String domain = URI.create(url).getHost().toLowerCase().replaceFirst("^www\\.", "");
            if (legacy) {
                safe.putIfAbsent("sourceId", safe.getOrDefault("id", "LEGACY_" + Integer.toUnsignedString(url.hashCode())));
                safe.putIfAbsent("domain", domain);
                safe.putIfAbsent("sourceVersion", "legacy-unknown");
                safe.putIfAbsent("lastReviewed", "legacy-unknown");
                safe.putIfAbsent("section", "legacy evidence");
                safe.putIfAbsent("heading", safe.get("section"));
                safe.putIfAbsent("matchedSymptoms", List.of());
                safe.putIfAbsent("matchedRules", List.of());
                safe.putIfAbsent("sourceStatus", "APPROVED");
                safe.putIfAbsent("retrievedAt", session.getCreatedAt().toString());
                safe.putIfAbsent("retrievalMode", "LOCAL");
            } else if (!hasCompleteCitationContract(safe, domain)) {
                continue;
            }
            valid.add(safe);
        }
        return valid;
    }

    private boolean hasCompleteCitationContract(Map<String, Object> citation, String expectedDomain) {
        return nonBlank(citation.get("sourceId")) != null
                && nonBlank(citation.get("domain")) != null
                && expectedDomain.equals(String.valueOf(citation.get("domain")).replaceFirst("^www\\.", ""))
                && nonBlank(citation.get("sourceVersion")) != null
                && nonBlank(citation.get("lastReviewed")) != null
                && (nonBlank(citation.get("section")) != null || nonBlank(citation.get("heading")) != null)
                && citation.get("matchedSymptoms") instanceof List<?>
                && citation.get("matchedRules") instanceof List<?>
                && java.util.Set.of("APPROVED", "PENDING_REVIEW").contains(nonBlank(citation.get("sourceStatus")))
                && nonBlank(citation.get("retrievedAt")) != null
                && java.util.Set.of("LOCAL", "REALTIME").contains(nonBlank(citation.get("retrievalMode")));
    }

    private List<Map<String, Object>> readValidatedClaims(
            IntakeSession session, List<Map<String, Object>> citations) {
        var evidenceIds = citations.stream().map(item -> nonBlank(item.get("sourceId")))
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> valid = new ArrayList<>();
        for (Map<String, Object> claim : readObjectList(session, "claims")) {
            String claimId = nonBlank(claim.get("claimId"));
            String text = nonBlank(claim.get("text"));
            if (claimId == null || text == null || !(claim.get("evidenceIds") instanceof List<?> raw)) continue;
            List<String> verified = raw.stream().map(this::nonBlank).filter(java.util.Objects::nonNull)
                    .filter(evidenceIds::contains).distinct().toList();
            if (!verified.isEmpty()) valid.add(Map.of("claimId", claimId, "text", text, "evidenceIds", verified));
        }
        return valid;
    }

    private boolean isOfficialHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return evidenceSourceService.isApprovedDeepLink(uri);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String nonBlank(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }
}
