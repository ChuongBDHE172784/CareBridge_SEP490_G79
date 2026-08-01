package com.carebridge.backend.triage.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagAudienceContext;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.integration.gemini.service.RagPolicyService;
import com.carebridge.backend.triage.TriageStage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Adds grounded, symptom-scoped guidance to a terminal triage result.
 *
 * <p>This boundary is deliberately fail-open: risk, emergency routing and the deterministic
 * triage result are already authoritative before it is called.  It never calls the public RAG
 * HTTP controller and it never turns private health-memory data into a RAG query.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TriageRagEnrichmentService {

    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_ANSWER_LENGTH = 4_000;
    private static final Set<String> QUERY_FIELDS = Set.of(
            "symptoms", "initialText", "parentFreeText", "symptomList", "duration",
            "temperatureC", "feedingStatus", "breathingStatus", "consciousnessStatus",
            "vomiting", "diarrhea", "rash", "seizure", "dehydrationSigns",
            "childAgeMonths", "newAnswers");
    private static final List<String> QUERY_FIELD_ORDER = List.of(
            "symptoms", "initialText", "parentFreeText", "symptomList", "duration",
            "temperatureC", "feedingStatus", "breathingStatus", "consciousnessStatus",
            "vomiting", "diarrhea", "rash", "seizure", "dehydrationSigns",
            "childAgeMonths", "newAnswers");
    private static final String NO_GROUNDED_SOURCE_WARNING =
            "Chưa tìm thấy nguồn tham khảo chính thống phù hợp với triệu chứng đã nhập.";
    private static final String UNSUPPORTED_STAGE_WARNING =
            "RAG hiện chỉ hỗ trợ nội dung cho giai đoạn thai kỳ/sau sinh; kết quả phân loại vẫn được giữ nguyên.";
    private static final String RAG_FAILURE_WARNING =
            "Không thể tải hướng dẫn tham khảo lúc này; kết quả phân loại vẫn được giữ nguyên.";

    private static final Set<String> RAG_FIELDS = Set.of("ragAnswer", "ragDisclaimer", "ragFallback");
    private static final List<String> SYMPTOM_FIELDS = List.of(
            "symptoms", "initialText", "parentFreeText", "symptomList");

    private final RagPolicyService ragPolicyService;
    private final EvidenceSourceService evidenceSourceService;
    private final ObjectMapper objectMapper;

    /** Enrich a one-shot result using the actual request fields. */
    public void enrichOneShot(
            Map<String, Object> result,
            TriageStage stage,
            UUID callerId,
            boolean mother,
            Map<String, Object> intake) {
        enrich(result, stage, callerId, mother, intake);
    }

    /** Enrich a conversation terminal result using initial/current answers. */
    public void enrichConversation(
            Map<String, Object> result,
            TriageStage stage,
            UUID callerId,
            boolean mother,
            String initialText,
            Map<String, Object> currentIntake,
            Map<String, Object> newAnswers) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (initialText != null) {
            values.put("initialText", initialText);
        }
        if (currentIntake != null) {
            values.putAll(currentIntake);
        }
        if (newAnswers != null && !newAnswers.isEmpty()) {
            values.put("newAnswers", newAnswers);
        }
        enrich(result, stage, callerId, mother, values);
    }

    private void enrich(
            Map<String, Object> result,
            TriageStage stage,
            UUID callerId,
            boolean mother,
            Map<String, Object> intake) {
        sanitizeExistingCitations(result);
        removeRagFields(result);
        if (!isTerminalNonRed(result)) {
            return;
        }
        if (stage == null || !stage.isMaternal()) {
            markFallback(result, UNSUPPORTED_STAGE_WARNING);
            return;
        }

        String symptomText = buildSymptomText(intake);
        if (symptomText.length() < 3) {
            markFallback(result, NO_GROUNDED_SOURCE_WARNING);
            return;
        }
        String query = buildRagQuery(intake, stage);

        try {
            RagAnswerResponse response = ragPolicyService.generateAnswer(
                    RagAnswerRequest.builder()
                            .query(query)
                            .userStage(toUserStage(stage))
                            .maxContextChunks(5)
                            .build(),
                    new RagAudienceContext(callerId, mother, toContentStage(stage)));
            if (response == null) {
                markFallback(result, RAG_FAILURE_WARNING);
                return;
            }
            List<Map<String, Object>> citations = approvedCitations(response.getSources(), symptomValues(intake));
            mergeCitations(result, citations);
            boolean grounded = !response.isFallback()
                    && response.getAnswer() != null
                    && !response.getAnswer().isBlank()
                    && !citations.isEmpty();
            if (grounded) {
                result.put("ragAnswer", cap(response.getAnswer(), MAX_ANSWER_LENGTH));
                if (response.getDisclaimer() != null && !response.getDisclaimer().isBlank()) {
                    result.put("ragDisclaimer", cap(response.getDisclaimer(), MAX_ANSWER_LENGTH));
                }
                result.put("ragFallback", false);
            } else {
                removeRagFields(result);
                result.put("ragFallback", true);
            }
            if (response.isFallback() || citations.isEmpty()) {
                appendWarning(result, NO_GROUNDED_SOURCE_WARNING);
            }
            if (response.getSources() != null && citations.size() < response.getSources().size()) {
                appendWarning(result, NO_GROUNDED_SOURCE_WARNING);
            }
        } catch (RuntimeException exception) {
            // Do not include query, health values or provider details in logs/responses.
            log.warn("Triage RAG enrichment unavailable reason={}",
                    exception.getClass().getSimpleName());
            markFallback(result, RAG_FAILURE_WARNING);
        }
    }

    private boolean isTerminalNonRed(Map<String, Object> result) {
        String status = text(result.get("status"));
        String risk = text(result.get("riskLevel"));
        return (status == null || "COMPLETED".equals(status) || "TRIAGE_COMPLETE".equals(status))
                && ("GREEN".equals(risk) || "YELLOW".equals(risk))
                && !Boolean.TRUE.equals(result.get("emergencyActionRequired"));
    }

    private UserStage toUserStage(TriageStage stage) {
        return switch (stage) {
            case PRECONCEPTION -> UserStage.PRE_PREGNANCY;
            case PREGNANCY -> UserStage.PREGNANCY;
            case POSTPARTUM -> UserStage.POSTPARTUM;
            default -> throw new IllegalArgumentException("Unsupported RAG triage stage");
        };
    }

    private String buildLegacyRagQuery(Map<String, Object> intake, TriageStage stage) {
        List<String> parts = new ArrayList<>();
        if (intake != null) {
            QUERY_FIELD_ORDER.forEach(key -> {
                if (intake.containsKey(key) && QUERY_FIELDS.contains(key)) {
                    appendValue(parts, intake.get(key));
                }
            });
        }
        String symptomText = String.join("; ", parts).replaceAll("\\s+", " ").trim();
        if (symptomText.isBlank()) {
            return "";
        }
        String query = symptomText + "; giai đoạn " + stage.name().toLowerCase(Locale.ROOT);
        String bounded = query.substring(0, Math.min(MAX_QUERY_LENGTH, query.length()));
        return stage == TriageStage.PRECONCEPTION
                ? bounded.replace("preconception", "pre_pregnancy") : bounded;
    }

    private String buildRagQuery(Map<String, Object> intake, TriageStage stage) {
        List<String> parts = new ArrayList<>();
        if (intake != null) {
            QUERY_FIELD_ORDER.forEach(key -> {
                if (intake.containsKey(key) && QUERY_FIELDS.contains(key)) {
                    appendValue(parts, intake.get(key));
                }
            });
        }
        String queryText = String.join("; ", parts).replaceAll("\\s+", " ").trim();
        if (queryText.isBlank()) return "";
        String suffix = "; stage=" + stageMarker(stage);
        int symptomBudget = Math.max(1, MAX_QUERY_LENGTH - suffix.length());
        String boundedSymptoms = queryText.length() <= symptomBudget
                ? queryText : queryText.substring(0, symptomBudget).trim();
        return boundedSymptoms + suffix;
    }

    private String stageMarker(TriageStage stage) {
        return switch (stage) {
            case PRECONCEPTION -> "pre_pregnancy";
            case PREGNANCY -> "pregnancy";
            case POSTPARTUM -> "postpartum";
            default -> stage.name().toLowerCase(Locale.ROOT);
        };
    }

    private String buildSymptomText(Map<String, Object> intake) {
        List<String> parts = new ArrayList<>();
        if (intake != null) {
            SYMPTOM_FIELDS.forEach(key -> appendValue(parts, intake.get(key)));
        }
        return String.join("; ", parts).replaceAll("\\s+", " ").trim();
    }

    private List<String> symptomValues(Map<String, Object> intake) {
        List<String> values = new ArrayList<>();
        if (intake != null) {
            SYMPTOM_FIELDS.forEach(key -> appendValue(values, intake.get(key)));
        }
        return values.stream().map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private ContentStage toContentStage(TriageStage stage) {
        return switch (stage) {
            case PRECONCEPTION -> ContentStage.PRE_PREGNANCY;
            case PREGNANCY -> ContentStage.PREGNANCY;
            case POSTPARTUM -> ContentStage.POSTPARTUM;
            default -> throw new IllegalArgumentException("Unsupported RAG triage stage");
        };
    }

    @SuppressWarnings("unchecked")
    private void appendValue(List<String> parts, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && !text.isBlank()) {
            parts.add(text.trim());
        } else if (value instanceof Number || value instanceof Boolean) {
            parts.add(String.valueOf(value));
        } else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                appendValue(parts, item);
            }
        } else if (value instanceof Map<?, ?> map) {
            map.forEach((key, nested) -> {
                if (QUERY_FIELDS.contains(String.valueOf(key))) {
                    appendValue(parts, nested);
                }
            });
        }
    }

    private List<Map<String, Object>> approvedCitations(List<RagSource> sources, List<String> matchedSymptoms) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> citations = new ArrayList<>();
        for (RagSource source : sources) {
            if (source == null || source.getUrl() == null || source.getUrl().isBlank()
                    || !isApprovedHttpsDeepLink(source.getUrl())) {
                continue;
            }
            String title = text(source.getTitle());
            if (title == null) {
                continue;
            }
            URI uri = URI.create(source.getUrl());
            String publisher = text(source.getPublisher());
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("sourceId", source.getContentId() == null
                    ? "RAG_" + Integer.toUnsignedString(source.getUrl().hashCode())
                    : source.getContentId().toString());
            citation.put("title", title);
            citation.put("source", publisher == null ? title : publisher);
            citation.put("organization", publisher == null ? title : publisher);
            citation.put("url", source.getUrl());
            citation.put("domain", uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", ""));
            citation.put("excerpt", text(source.getExcerpt()) == null ? title : source.getExcerpt());
            String retrievedAt = Instant.now().toString();
            citation.put("retrievedAt", retrievedAt);
            citation.put("matchedSymptoms", matchedSymptoms);
            citation.put("matchedRules", List.of());
            citation.put("sourceStatus", "APPROVED");
            if (text(source.getSourceVersion()) == null || text(source.getLastReviewed()) == null) {
                // The strict triage citation contract requires review provenance. Do not
                // invent a generic citation when the retrieved item cannot provide it.
                continue;
            }
            citation.put("sourceVersion", source.getSourceVersion());
            citation.put("lastReviewed", source.getLastReviewed());
            citation.put("section", "RAG approved content");
            citation.put("heading", "RAG approved content");
            citation.put("retrievalMode", "LOCAL");
            citations.add(citation);
        }
        return citations;
    }

    private boolean isApprovedHttpsDeepLink(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null
                    ? "" : uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            boolean genericSearch = path.matches(".*/(search|query|find)(/.*)?/?")
                    || Set.of("google.com", "bing.com", "yahoo.com").contains(host);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && (uri.getUserInfo() == null || uri.getUserInfo().isBlank())
                    && !genericSearch
                    && evidenceSourceService.isApprovedDeepLink(uri);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeCitations(Map<String, Object> result, List<Map<String, Object>> additions) {
        List<Map<String, Object>> merged = new ArrayList<>();
        Object existing = result.get("citations");
        if (existing instanceof List<?> list) {
            list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> objectMapper.convertValue(item,
                            new TypeReference<Map<String, Object>>() {}))
                    .forEach(merged::add);
        }
        merged.addAll(additions);
        List<Map<String, Object>> deduplicated = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (Map<String, Object> citation : merged) {
            String key = citationKey(citation);
            if (key != null && seen.add(key)) {
                deduplicated.add(citation);
            }
        }
        if (deduplicated.isEmpty()) {
            result.remove("citations");
        } else {
            result.put("citations", deduplicated);
        }
    }

    private void markFallback(Map<String, Object> result, String warning) {
        removeRagFields(result);
        result.put("ragFallback", true);
        appendWarning(result, warning);
    }

    private void removeRagFields(Map<String, Object> result) {
        RAG_FIELDS.forEach(result::remove);
    }

    private void sanitizeExistingCitations(Map<String, Object> result) {
        Object existing = result.get("citations");
        if (!(existing instanceof List<?> list)) {
            return;
        }
        List<Map<String, Object>> safe = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> citation = objectMapper.convertValue(
                    map, new TypeReference<Map<String, Object>>() {});
            String title = text(citation.get("title"));
            String source = text(citation.get("organization"));
            if (source == null) source = text(citation.get("source"));
            String url = text(citation.get("url"));
            String excerpt = text(citation.get("excerpt"));
            if (title == null || source == null || url == null || excerpt == null
                    || !isApprovedHttpsDeepLink(url)) {
                continue;
            }
            safe.add(citation);
        }
        if (safe.isEmpty()) result.remove("citations");
        else result.put("citations", safe);
    }

    private String citationKey(Map<String, Object> citation) {
        String url = text(citation.get("url"));
        if (url != null) return url.toLowerCase(Locale.ROOT);
        String sourceId = text(citation.get("sourceId"));
        if (sourceId != null) return sourceId;
        return text(citation.get("title"));
    }

    private String cap(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max).trim();
    }

    private void appendWarning(Map<String, Object> result, String warning) {
        String existing = text(result.get("warning"));
        result.put("warning", existing == null ? warning : existing + " " + warning);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
