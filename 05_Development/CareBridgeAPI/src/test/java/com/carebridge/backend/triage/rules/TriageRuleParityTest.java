package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Java/Python rule parity for canonical registry v2.1.0.
 *
 * <p>Both runtimes load the same registry file and the same vectors; Python asserts them in
 * {@code tests/test_rule_registry_parity_v2.py}. Expectations derive from the approved Rule
 * Matrix plus the Phase 2.5 safety policies — never edit one to turn a test green.
 */
class TriageRuleParityTest {

    private static final String VECTORS_RESOURCE = "triage/triage_rule_parity_vectors_v2.json";

    private static TriageRuleRegistry registry;
    private static TriageRuleEvaluator evaluator;
    private static JsonNode vectorsDocument;

    @BeforeAll
    static void loadCanonicalArtifacts() throws IOException {
        registry = new TriageRuleRegistry();
        evaluator = new TriageRuleEvaluator(registry);
        try (InputStream stream = new ClassPathResource(VECTORS_RESOURCE).getInputStream()) {
            vectorsDocument = new ObjectMapper().readTree(stream);
        }
    }

    static Stream<JsonNode> vectors() throws IOException {
        try (InputStream stream = new ClassPathResource(VECTORS_RESOURCE).getInputStream()) {
            List<JsonNode> vectors = new ArrayList<>();
            new ObjectMapper().readTree(stream).get("vectors").forEach(vectors::add);
            return vectors.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vectors")
    @DisplayName("Java evaluator matches the shared parity vector")
    void javaEvaluatorMatchesSharedVector(JsonNode vector) {
        JsonNode input = vector.get("input");
        JsonNode expected = vector.get("expected");
        String label = vector.path("id").asText() + ": " + vector.path("description").asText();

        TriageRuleEvaluator.RuleEvaluation evaluation = evaluator.evaluate(
                input.get("stage").asText(),
                toValueMap(input.get("signals")),
                toValueMap(input.get("context")),
                input.get("questionRound").asInt(),
                input.get("reproductiveRelevance").asBoolean(),
                input.get("minimumDatasetComplete").asBoolean());

        assertThat(evaluation.outcome()).as(label).isEqualTo(expected.get("outcome").asText());
        assertThat(evaluation.decisiveRuleIds()).as(label)
                .containsExactlyElementsOf(textList(expected.get("decisiveRuleIds")));
        assertThat(evaluation.stopConversation()).as(label)
                .isEqualTo(expected.get("stopConversation").asBoolean());
        assertThat(evaluation.actionCode()).as(label)
                .isEqualTo(expected.get("actionCode").asText());

        assertThat(evaluation.reasonCodes()).as(label)
                .containsAll(textList(expected.get("reasonCodes")));
        assertThat(evaluation.greenBlockedBy()).as(label)
                .containsAll(textList(expected.get("greenBlockedBy")));
        assertThat(evaluation.pendingRedRuleIds()).as(label)
                .containsAll(textList(expected.get("pendingRedRuleIds")));
        assertThat(evaluation.unresolvedSignals()).as(label)
                .containsAll(textList(expected.get("unresolvedSignals")));
        assertThat(evaluation.suppressedRuleIds()).as(label)
                .containsAll(textList(expected.get("suppressedRuleIds")));
    }

    // ------------------------------------------------------------------ registry

    @Test
    @DisplayName("Registry loads v2.2.0 with the ten internally reviewed rules")
    void registryLoadsTheInternalRuleSet() {
        assertThat(registry.rulesetVersion()).isEqualTo("2.2.0");
        assertThat(registry.ruleMatrixVersion()).isEqualTo("0.1.0");
        // 10 maternal rows from the v0.1.0 matrix plus 7 paediatric rules ported from V1. Counted
        // separately so a maternal row going missing still fails loudly.
        assertThat(registry.rules().stream()
                .filter(rule -> !rule.ruleId().startsWith("PED_")).toList()).hasSize(10);
        assertThat(registry.rules().stream()
                .filter(rule -> rule.ruleId().startsWith("PED_")).toList()).hasSize(7);
        assertThat(registry.skippedRuleIds()).isEmpty();
        assertThat(vectorsDocument.get("rulesetVersion").asText())
                .isEqualTo(registry.rulesetVersion());
    }

    @Test
    @DisplayName("GREEN ships locked by default")
    void greenReleaseGateIsLocked() {
        assertThat(registry.greenEnabled()).isFalse();
    }

    @Test
    @DisplayName("Safety policies stay outside the clinical rule set")
    void safetyPoliciesAreNotClinicalRules() {
        assertThat(registry.safetyPolicies())
                .extracting(TriageSafetyPolicy::policyId)
                .containsExactlyInAnyOrder("SAFETY_CYANOSIS_HOLDOVER_001", "SAFETY_SELF_HARM_001");
        for (TriageSafetyPolicy policy : registry.safetyPolicies()) {
            assertThat(policy.status()).as(policy.policyId()).isNotEqualTo("APPROVED");
            assertThat(policy.reviewDueAt()).as(policy.policyId()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Cyanosis is not folded into GLOBAL_RED_001")
    void cyanosisIsNotInsideGlobalRed() {
        TriageRule rule = registry.byId("GLOBAL_RED_001").orElseThrow();
        assertThat(rule.condition().toString()).doesNotContain("CYANOSIS");
    }

    @Test
    @DisplayName("SYS_INFO_001 keeps the Matrix stop flag (FALSE) and its Matrix action")
    void sysInfoKeepsMatrixFlags() {
        TriageRule rule = registry.byId("SYS_INFO_001").orElseThrow();
        assertThat(rule.stopOnMatch()).isFalse();
        assertThat(rule.actionCode()).isEqualTo("ASK_CLARIFYING_QUESTIONS");
    }

    @Test
    @DisplayName("A tampered runtime copy is refused")
    void tamperedCopyIsRefused() {
        assertThatThrownBy(() -> new TriageRuleRegistry(
                "triage/triage_rule_parity_vectors_v2.json", TriageRuleRegistry.MANIFEST_RESOURCE,
                java.time.LocalDate.now()))
                .isInstanceOf(TriageRuleRegistry.RegistryIntegrityException.class);
    }

    // ----------------------------------------------------------------- tri-state

    @Test
    @DisplayName("Kleene truth tables")
    void kleeneTruthTables() {
        assertThat(Tri.TRUE.and(Tri.UNKNOWN)).isEqualTo(Tri.UNKNOWN);
        assertThat(Tri.FALSE.and(Tri.UNKNOWN)).isEqualTo(Tri.FALSE);
        assertThat(Tri.TRUE.or(Tri.UNKNOWN)).isEqualTo(Tri.TRUE);
        assertThat(Tri.FALSE.or(Tri.UNKNOWN)).isEqualTo(Tri.UNKNOWN);
        assertThat(Tri.UNKNOWN.negate()).isEqualTo(Tri.UNKNOWN);
    }

    @Test
    @DisplayName("An unanswered question never proves a symptom is absent")
    void notOfMissingSignalIsNeverTrue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"not\":{\"signal\":\"SEIZURE\",\"operator\":\"EQ\",\"value\":true}}");
        assertThat(RuleConditionEvaluator.evaluate(node, Map.of(), Map.of())).isEqualTo(Tri.UNKNOWN);

        JsonNode neq = mapper.readTree("{\"signal\":\"SEIZURE\",\"operator\":\"NEQ\",\"value\":true}");
        assertThat(RuleConditionEvaluator.evaluate(neq, Map.of(), Map.of())).isEqualTo(Tri.UNKNOWN);
    }

    @Test
    @DisplayName("CONFLICTED and UNAWARE_OR_UNMEASURABLE behave as UNKNOWN, never as ABSENT")
    void unresolvedPresencesBehaveAsUnknown() throws IOException {
        JsonNode node = new ObjectMapper()
                .readTree("{\"signal\":\"VAGINAL_BLEEDING\",\"operator\":\"EQ\",\"value\":true}");
        assertThat(RuleConditionEvaluator.evaluate(node, Map.of("VAGINAL_BLEEDING", "CONFLICTED"), Map.of()))
                .isEqualTo(Tri.UNKNOWN);
        assertThat(RuleConditionEvaluator.evaluate(
                node, Map.of("VAGINAL_BLEEDING", "UNAWARE_OR_UNMEASURABLE"), Map.of()))
                .isEqualTo(Tri.UNKNOWN);
        assertThat(RuleConditionEvaluator.evaluate(node, Map.of("VAGINAL_BLEEDING", "ABSENT"), Map.of()))
                .isEqualTo(Tri.FALSE);
    }

    @Test
    @DisplayName("Condition DSL rejects free expressions, unknown operators and empty groups")
    void conditionSchemaIsClosed() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        for (String bad : List.of(
                "{\"expression\":\"signals['SEIZURE'] == True\"}",
                "{\"signal\":\"SEIZURE\",\"operator\":\"MATCHES\",\"value\":\".*\"}",
                "{\"any\":[]}")) {
            assertThatThrownBy(() ->
                    RuleConditionEvaluator.validateCondition(mapper.readTree(bad), "condition"))
                    .isInstanceOf(RuleConditionEvaluator.ConditionSchemaException.class);
        }
    }

    // -------------------------------------------------------------- green lock

    @Test
    @DisplayName("GREEN is unreachable at every round while the gate is locked")
    void greenIsUnreachableWhileLocked() {
        for (int round = 0; round <= 4; round++) {
            for (boolean complete : List.of(true, false)) {
                assertThat(evaluator.evaluate("PREGNANCY", Map.of(), Map.of(), round, true, complete)
                        .outcome()).as("round %d complete %s", round, complete).isNotEqualTo("GREEN");
            }
        }
    }

    @Test
    @DisplayName("Audit trace records suppressed rules, not just the decisive one")
    void auditTraceRecordsSuppressedRules() {
        TriageRuleEvaluator.RuleEvaluation evaluation = evaluator.evaluate(
                "PREGNANCY",
                Map.of("HEAVY_VAGINAL_BLEEDING", "PRESENT", "VAGINAL_BLEEDING", "PRESENT"),
                Map.of("gestational_week", 20, "bleeding_amount", "HEAVY"),
                1, true, true);
        assertThat(evaluation.decisiveRuleIds()).containsExactly("PREG_RED_001");
        assertThat(evaluation.suppressedRuleIds()).contains("PREG_YELLOW_001");
    }

    @Test
    @DisplayName("Reordering the registry cannot change a decision")
    void tieBreakIsIndependentOfFileOrder() {
        List<TriageRule> forward = registry.forStage("POSTPARTUM");
        List<TriageRule> reversed = new ArrayList<>(forward);
        java.util.Collections.reverse(reversed);
        assertThat(TriageRuleRegistry.sortedByPrecedence(reversed))
                .containsExactlyElementsOf(TriageRuleRegistry.sortedByPrecedence(forward));
    }

    // ------------------------------------------------------------------ helpers

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

    private static List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(element -> values.add(element.asText()));
        }
        return values;
    }
}
