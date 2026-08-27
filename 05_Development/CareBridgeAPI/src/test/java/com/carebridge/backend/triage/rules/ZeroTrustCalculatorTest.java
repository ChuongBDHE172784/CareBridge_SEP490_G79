package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Targeted tests for the zero-trust calculators (dataset, scope, pending risk, exclusion
 * audit). These were previously exercised only indirectly through the shared parity vectors,
 * which cannot show *why* a status was reached.
 *
 * <p>The premise under test: a caller — frontend, another service, or a future LLM extractor —
 * can assert anything it likes, and the engine must ignore it and compute from structured
 * state. Covers PHASE0-TEST-001..004.
 */
class ZeroTrustCalculatorTest {

    private static TriageRuleEvaluator evaluator;

    @BeforeAll
    static void setUp() {
        evaluator = new TriageRuleEvaluator(new TriageRuleRegistry());
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        return values;
    }

    /** Every global safety signal explicitly denied — the only way to a complete screen. */
    private static Map<String, Object> allSafetySignalsAbsent() {
        Map<String, Object> signals = new LinkedHashMap<>();
        IndependentGlobalSafetyFallback.GLOBAL_DANGER_SIGNALS.keySet()
                .forEach(code -> signals.put(code, "ABSENT"));
        return signals;
    }

    // ------------------------------------------------------- dataset (PHASE0-TEST-001)

    @Test
    @DisplayName("A caller claiming the dataset is complete does not make it complete")
    void callerClaimIsIgnoredWhenSignalsAreUnknown() {
        var evaluation = evaluator.evaluate("PREGNANCY", map(), map(), 1, true, /*caller says*/ true);

        assertThat(evaluation.greenEligibilityDatasetStatus()).isNotEqualTo("COMPLETE");
        assertThat(evaluation.outcome()).isNotEqualTo("GREEN");
        assertThat(evaluation.auditMismatches())
                .as("the caller's false claim must be recorded, not silently dropped")
                .isNotEmpty();
    }

    @Test
    @DisplayName("A caller claiming the dataset is incomplete does not block a complete one")
    void engineComputesCompletenessIndependentlyOfTheCaller() {
        Map<String, Object> signals = allSafetySignalsAbsent();
        Map<String, Object> context = map(
                "postpartum_day", 20, "bleeding_amount", "NONE",
                "pain_severity", "MILD", "current_status", "ONGOING", "delivery_method", "VAGINAL");

        var claimedIncomplete = evaluator.evaluate("POSTPARTUM", signals, context, 2, true, false);
        var claimedComplete = evaluator.evaluate("POSTPARTUM", signals, context, 2, true, true);

        assertThat(claimedIncomplete.safetyScreenStatus())
                .as("the caller flag must not change a computed status")
                .isEqualTo(claimedComplete.safetyScreenStatus());
        assertThat(claimedIncomplete.outcome()).isEqualTo(claimedComplete.outcome());
    }

    @Test
    @DisplayName("A missing signal is UNKNOWN, never ABSENT")
    void missingSignalIsNotTreatedAsDenied() {
        var missing = evaluator.evaluate("PREGNANCY", map(), map(), 1, true, true);
        var denied = evaluator.evaluate("PREGNANCY", allSafetySignalsAbsent(), map(), 1, true, true);

        assertThat(missing.safetyScreenStatus())
                .as("an unasked question must not count as a negative answer")
                .isNotEqualTo(denied.safetyScreenStatus());
    }

    @Test
    @DisplayName("A contradictory signal is reported as a conflict, not resolved")
    void conflictedSignalIsSurfaced() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("VAGINAL_BLEEDING", "CONFLICTED"),
                map("gestational_week", 15, "bleeding_amount", "NONE"), 2, true, true);

        assertThat(evaluation.dataConflicts()).anyMatch(c -> c.contains("VAGINAL_BLEEDING"));
        assertThat(evaluation.outcome()).isNotIn("GREEN", "OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("An unmeasurable answer counts as unresolved, not as normal")
    void unawareIsUnresolvedNotNormal() {
        Map<String, Object> signals = allSafetySignalsAbsent();
        signals.put("SEVERE_BREATHING_DIFFICULTY", "UNAWARE_OR_UNMEASURABLE");

        var evaluation = evaluator.evaluate("PREGNANCY", signals, map("gestational_week", 30),
                2, true, true);

        assertThat(evaluation.safetyScreenStatus()).isNotEqualTo("COMPLETE");
        assertThat(evaluation.outcome()).isNotEqualTo("GREEN");
    }

    @Test
    @DisplayName("A historical signal does not become a current one")
    void historicalSignalIsNotCurrent() {
        var evaluation = evaluator.evaluate("POSTPARTUM",
                map("SEIZURE", Map.of("presence", "PRESENT", "current", false)),
                map("postpartum_day", 10), 1, true, false);

        assertThat(evaluation.outcome())
                .as("a past seizure carried in from health memory is not an emergency now")
                .isNotEqualTo("RED");
    }

    // --------------------------------------------------------- scope (PHASE0-TEST-002)

    @Test
    @DisplayName("OUT_OF_SCOPE needs a complete safety screen, not merely an unrecognised complaint")
    void incompleteSafetyScreenBlocksOutOfScope() {
        var evaluation = evaluator.evaluate("PRECONCEPTION", map(), map(), 1, false, true);

        assertThat(evaluation.outcome())
                .as("nothing is known yet, so nothing can be ruled out of scope")
                .isNotEqualTo("OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("A caller saying 'not reproductive' against reproductive evidence is a conflict")
    void callerScopeClaimAgainstEvidenceBecomesConflict() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                allSafetySignalsAbsent(), map("gestational_week", 20), 1, false, true);

        assertThat(evaluation.outcome()).isNotEqualTo("OUT_OF_SCOPE");
        assertThat(evaluation.scopeStatus()).isEqualTo(ScopeStatus.CONFLICTED.name());
    }

    @Test
    @DisplayName("An unresolved possible pregnancy is never out of scope")
    void unresolvedPossiblePregnancyIsNotOutOfScope() {
        var evaluation = evaluator.evaluate("POSSIBLE_PREGNANCY",
                allSafetySignalsAbsent(), map("possible_pregnancy", "UNKNOWN"), 1, false, true);

        assertThat(evaluation.outcome()).isNotEqualTo("OUT_OF_SCOPE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Flipping the caller's relevance flag alone never changes the outcome")
    void callerRelevanceFlagAloneChangesNothing(boolean relevance) {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("HEAVY_VAGINAL_BLEEDING", "PRESENT"),
                map("gestational_week", 30, "bleeding_amount", "HEAVY"), 1, relevance, true);

        assertThat(evaluation.outcome()).isEqualTo("RED");
        assertThat(evaluation.decisiveRuleIds()).containsExactly("PREG_RED_001");
    }

    // -------------------------------------------------- pending risk (PHASE0-TEST-003)

    @Test
    @DisplayName("A half-satisfied RED rule is asked about while rounds remain")
    void pendingRedAsksWhileRoundsRemain() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("SEVERE_HEADACHE", "PRESENT"), map("gestational_week", 22), 1, true, true);

        assertThat(evaluation.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(evaluation.stopConversation()).isFalse();
        assertThat(evaluation.pendingRedRuleIds()).contains("PREG_RED_002");
        assertThat(evaluation.unresolvedSignals()).contains("VISUAL_DISTURBANCE");
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    @DisplayName("A half-satisfied RED rule escalates once the round budget is spent")
    void pendingRedEscalatesWhenRoundsAreSpent(int round) {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("SEVERE_HEADACHE", "PRESENT", "VISUAL_DISTURBANCE", "UNAWARE_OR_UNMEASURABLE"),
                map("gestational_week", 28), round, true, true);

        assertThat(evaluation.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(evaluation.stopConversation()).isTrue();
        assertThat(evaluation.actionCode()).isEqualTo(TriageRuleEvaluator.ROUTE_TO_HEALTHCARE_WORKER);
        assertThat(evaluation.outcome()).isNotIn("GREEN", "OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("An unresolved RED condition outranks a lesser pending status")
    void unresolvedRedTakesPrecedence() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("SEVERE_HEADACHE", "PRESENT"), map(), 2, true, false);

        assertThat(evaluation.primaryPendingRiskStatus())
                .isEqualTo(PendingRiskStatus.UNRESOLVED_RED_CONDITION.name());
    }

    // ---------------------------------------------- exclusion audit (PHASE0-TEST-004)

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "CONFLICTED"})
    @DisplayName("An ambiguous bleeding amount suppresses YELLOW but stays in the trace")
    void suppressedRuleRemainsAuditable(String amount) {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("VAGINAL_BLEEDING", "PRESENT"),
                map("gestational_week", 10, "bleeding_amount", amount), 1, true, true);

        assertThat(evaluation.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(evaluation.allMatchedRules())
                .as("a suppressed rule must never vanish from the audit trail")
                .anySatisfy(trace -> {
                    assertThat(trace.ruleId()).isEqualTo("PREG_YELLOW_001");
                    assertThat(trace.role()).isEqualTo("SUPPRESSED_BY_EXCLUSION");
                    assertThat(trace.suppressionReason()).isNotNull();
                });
    }

    @Test
    @DisplayName("A clear bleeding amount does not suppress YELLOW")
    void unambiguousDataDoesNotSuppress() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("VAGINAL_BLEEDING", "PRESENT", "HEAVY_VAGINAL_BLEEDING", "ABSENT"),
                map("gestational_week", 10, "bleeding_amount", "SPOTTING"), 2, true, true);

        assertThat(evaluation.outcome()).isEqualTo("YELLOW");
        assertThat(evaluation.decisiveRuleIds()).containsExactly("PREG_YELLOW_001");
    }

    @Test
    @DisplayName("A higher-severity match suppresses YELLOW with a different recorded role")
    void severitySuppressionIsDistinctFromExclusion() {
        var evaluation = evaluator.evaluate("PREGNANCY",
                map("VAGINAL_BLEEDING", "PRESENT", "HEAVY_VAGINAL_BLEEDING", "PRESENT"),
                map("gestational_week", 20, "bleeding_amount", "HEAVY"), 1, true, true);

        assertThat(evaluation.allMatchedRules())
                .anySatisfy(trace -> {
                    assertThat(trace.ruleId()).isEqualTo("PREG_YELLOW_001");
                    assertThat(trace.role()).isEqualTo("SUPPRESSED_BY_HIGHER_SEVERITY");
                });
    }
}
