package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-result parity: Java and Python must agree on EVERY result field, not just the four
 * mandatory ones.
 *
 * <p>The per-vector assertions in {@link TriageRuleParityV2Test} check the optional lists by
 * containment, so an extra reason code or blocker appearing on one runtime only would pass
 * unnoticed. Here both runtimes canonicalise the complete result for all shared vectors and
 * hash it; any divergence changes the digest on one side and fails.
 *
 * <p>The Python counterpart is {@code test_full_result_parity_fingerprint}.
 */
class ParityResultFingerprintTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VECTORS_RESOURCE = "triage/triage_rule_parity_vectors_v2.json";
    private static final Path FINGERPRINT = Path.of("..", "Contracts", "triage",
            "parity_result_fingerprint.json");

    @Test
    @DisplayName("Java produces the same full-result digest as Python")
    void javaMatchesTheSharedFingerprint() throws Exception {
        JsonNode expected = MAPPER.readTree(Files.readString(FINGERPRINT));

        TriageRuleRegistry registry = new TriageRuleRegistry();
        TriageRuleEvaluator evaluator = new TriageRuleEvaluator(registry);

        List<JsonNode> vectors = new ArrayList<>();
        try (InputStream stream = new ClassPathResource(VECTORS_RESOURCE).getInputStream()) {
            MAPPER.readTree(stream).get("vectors").forEach(vectors::add);
        }
        assertThat(vectors).hasSize(expected.get("vectorCount").asInt());

        ArrayNode rows = MAPPER.createArrayNode();
        for (JsonNode vector : vectors) {
            JsonNode input = vector.get("input");
            TriageRuleEvaluator.RuleEvaluation evaluation = evaluator.evaluate(
                    input.get("stage").asText(),
                    toValueMap(input.get("signals")),
                    toValueMap(input.get("context")),
                    input.get("questionRound").asInt(),
                    input.get("reproductiveRelevance").asBoolean(),
                    input.get("minimumDatasetComplete").asBoolean());

            ObjectNode row = MAPPER.createObjectNode();
            row.put("id", vector.get("id").asText());
            row.put("outcome", evaluation.outcome());
            row.set("decisiveRuleIds", asArray(evaluation.decisiveRuleIds(), false));
            row.put("stopConversation", evaluation.stopConversation());
            row.put("actionCode", evaluation.actionCode());
            row.set("reasonCodes", asArray(evaluation.reasonCodes(), true));
            row.set("questionIds", asArray(evaluation.questionIds(), true));
            row.set("requiredFields", asArray(evaluation.requiredFields(), true));
            row.set("greenBlockedBy", asArray(evaluation.greenBlockedBy(), true));
            row.set("pendingRedRuleIds", asArray(evaluation.pendingRedRuleIds(), true));
            row.set("unresolvedSignals", asArray(evaluation.unresolvedSignals(), true));
            row.set("suppressedRuleIds", asArray(evaluation.suppressedRuleIds(), true));
            rows.add(row);
        }

        String digest = sha256(canonicalise(rows));
        assertThat(digest)
                .as("Java and Python engine output diverged on a field the per-vector "
                        + "containment assertions cannot see. Investigate before regenerating.")
                .isEqualTo(expected.get("fingerprint").asText());
    }

    /**
     * Sorted-key JSON with no whitespace — must match Python's
     * {@code json.dumps(..., sort_keys=True, separators=(',',':'))} byte for byte.
     */
    private static String canonicalise(JsonNode node) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            StringBuilder builder = new StringBuilder("{");
            for (int index = 0; index < names.size(); index++) {
                if (index > 0) builder.append(',');
                builder.append(quote(names.get(index))).append(':')
                        .append(canonicalise(node.get(names.get(index))));
            }
            return builder.append('}').toString();
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder("[");
            for (int index = 0; index < node.size(); index++) {
                if (index > 0) builder.append(',');
                builder.append(canonicalise(node.get(index)));
            }
            return builder.append(']').toString();
        }
        if (node.isTextual()) {
            return quote(node.asText());
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "true" : "false";
        }
        return node.asText();
    }

    private static String quote(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                default -> builder.append(character);
            }
        }
        return builder.append('"').toString();
    }

    private static ArrayNode asArray(List<String> values, boolean sorted) {
        List<String> copy = new ArrayList<>(values);
        if (sorted) {
            copy.sort(String::compareTo);
        }
        ArrayNode array = MAPPER.createArrayNode();
        copy.forEach(array::add);
        return array;
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Object> toValueMap(JsonNode node) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return values;
        }
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isNull()) {
                values.put(entry.getKey(), null);
            } else if (value.isBoolean()) {
                values.put(entry.getKey(), value.asBoolean());
            } else if (value.isIntegralNumber()) {
                values.put(entry.getKey(), value.asInt());
            } else if (value.isNumber()) {
                values.put(entry.getKey(), value.asDouble());
            } else {
                values.put(entry.getKey(), value.asText());
            }
        });
        return values;
    }
}
