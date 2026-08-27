package com.carebridge.backend.aimoderation.policy;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.dto.AiVerdictMatch;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.aimoderation.exception.AiVerdictParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strict server-side validation of the model's JSON verdict. Nothing the model says is
 * trusted verbatim: unknown policy codes are dropped, category/severity are re-read from the
 * server policy rows, and evidence must be a real excerpt of the scanned content. Invalid
 * JSON or schema violations raise {@link AiVerdictParseException} (a retryable failure —
 * never silently SAFE). String-contains parsing of model output is deliberately absent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiVerdictParser {

    private static final int MAX_MATCHES = 10;
    private static final int MAX_EVIDENCE_PER_MATCH = 3;
    private static final int MAX_EVIDENCE_LENGTH = 200;
    private static final int MAX_EXPLANATION_LENGTH = 500;
    private static final int MAX_OVERALL_EXPLANATION_LENGTH = 1000;

    private final ObjectMapper objectMapper;

    public AiVerdict parse(String rawJson, Map<String, AiModerationPolicy> policiesByCode, String scannedContent) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new AiVerdictParseException("Model response is not valid JSON", ex);
        }
        if (root == null || !root.isObject()) {
            throw new AiVerdictParseException("Model response is not a JSON object");
        }

        AiClassification classification = requiredEnum(root, "classification", AiClassification.class);
        BigDecimal confidence = requiredConfidence(root, "confidence");

        JsonNode matchesNode = root.get("matchedPolicies");
        if (matchesNode == null || !matchesNode.isArray()) {
            throw new AiVerdictParseException("matchedPolicies must be an array");
        }

        String normalizedContent = normalizeForEvidence(scannedContent);
        List<AiVerdictMatch> matches = new ArrayList<>();
        for (JsonNode matchNode : matchesNode) {
            if (matches.size() >= MAX_MATCHES) {
                break;
            }
            parseMatch(matchNode, policiesByCode, normalizedContent).ifPresent(matches::add);
        }

        AiRecommendedAction recommendedAction = optionalEnum(root, "recommendedAction", AiRecommendedAction.class);
        AiPolicySeverity overallSeverity = optionalEnum(root, "overallSeverity", AiPolicySeverity.class);
        if (overallSeverity == null) {
            overallSeverity = matches.stream()
                    .map(AiVerdictMatch::severity)
                    .max(Enum::compareTo)
                    .orElse(null);
        }

        // A VIOLATION claim with no surviving validated match is not actionable evidence —
        // downgrade to UNCERTAIN so it can still reach human review via the threshold path.
        if (classification == AiClassification.VIOLATION && matches.isEmpty()) {
            classification = AiClassification.UNCERTAIN;
        }

        String explanation = truncate(textOrNull(root, "explanation"), MAX_OVERALL_EXPLANATION_LENGTH);
        return new AiVerdict(classification, overallSeverity, confidence, List.copyOf(matches),
                recommendedAction, explanation);
    }

    private java.util.Optional<AiVerdictMatch> parseMatch(JsonNode matchNode,
            Map<String, AiModerationPolicy> policiesByCode, String normalizedContent) {
        if (matchNode == null || !matchNode.isObject()) {
            return java.util.Optional.empty();
        }
        String policyCode = textOrNull(matchNode, "policyCode");
        if (policyCode == null) {
            return java.util.Optional.empty();
        }
        AiModerationPolicy policy = policiesByCode.get(policyCode.trim().toUpperCase(Locale.ROOT));
        if (policy == null) {
            log.debug("Dropping match with unknown policyCode from model output");
            return java.util.Optional.empty();
        }
        JsonNode confidenceNode = matchNode.get("confidence");
        if (confidenceNode == null || !confidenceNode.isNumber()) {
            return java.util.Optional.empty();
        }
        BigDecimal confidence = toConfidence(confidenceNode.decimalValue());
        if (confidence == null) {
            return java.util.Optional.empty();
        }

        List<String> evidence = new ArrayList<>();
        JsonNode evidenceNode = matchNode.get("evidence");
        if (evidenceNode != null && evidenceNode.isArray()) {
            for (JsonNode item : evidenceNode) {
                if (evidence.size() >= MAX_EVIDENCE_PER_MATCH) {
                    break;
                }
                if (!item.isTextual()) {
                    continue;
                }
                String excerpt = item.asText().strip();
                if (excerpt.isEmpty() || excerpt.length() > MAX_EVIDENCE_LENGTH) {
                    continue;
                }
                // Evidence must actually occur in the scanned content; fabricated excerpts are dropped.
                if (normalizedContent.contains(normalizeForEvidence(excerpt))) {
                    evidence.add(excerpt);
                }
            }
        }

        String explanation = truncate(textOrNull(matchNode, "explanation"), MAX_EXPLANATION_LENGTH);
        // policy id/code/version + category/severity come from the server-side policy row —
        // an immutable snapshot for matches_jsonb, never trusted from model output
        return java.util.Optional.of(new AiVerdictMatch(policy.getId(), policy.getPolicyCode(),
                policy.getVersion(), policy.getViolationCategory(), policy.getSeverity(), confidence,
                List.copyOf(evidence), explanation));
    }

    private <E extends Enum<E>> E requiredEnum(JsonNode root, String field, Class<E> type) {
        String value = textOrNull(root, field);
        if (value == null) {
            throw new AiVerdictParseException("Missing required field: " + field);
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AiVerdictParseException("Invalid value for " + field);
        }
    }

    private <E extends Enum<E>> E optionalEnum(JsonNode root, String field, Class<E> type) {
        String value = textOrNull(root, field);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BigDecimal requiredConfidence(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            throw new AiVerdictParseException("Missing or non-numeric field: " + field);
        }
        BigDecimal value = toConfidence(node.decimalValue());
        if (value == null) {
            throw new AiVerdictParseException("Field " + field + " must be between 0 and 1");
        }
        return value;
    }

    private static BigDecimal toConfidence(BigDecimal raw) {
        if (raw == null || raw.compareTo(BigDecimal.ZERO) < 0 || raw.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return raw.setScale(3, RoundingMode.HALF_UP);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    /** Case-insensitive, whitespace-collapsed comparison space for evidence verification. */
    static String normalizeForEvidence(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }
}
