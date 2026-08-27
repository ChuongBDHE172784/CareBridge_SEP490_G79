package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * One development-reviewed, not-clinically-validated rule from the canonical registry. Mirrors
 * {@code app/rules/registry.py::Rule}; both sides load the SAME file, so this type must
 * never carry a hand-written clinical default.
 *
 * @param stopOnMatch   the Matrix "Dừng hỏi" column — the rule's OWN flag. Stopping once
 *                      the question budget is spent is a separate RUNTIME policy and must
 *                      not be folded into this boolean.
 * @param decisionOrder explicit tie-break. Physical position in the JSON is deliberately
 *                      NOT used, so reordering the file cannot change a clinical decision.
 */
public record TriageRule(
        String ruleId,
        String ruleVersion,
        String ruleType,
        String title,
        List<String> stages,
        String outcome,
        int priority,
        int decisionOrder,
        boolean stopOnMatch,
        String conditionType,
        JsonNode condition,
        List<String> requiredFields,
        List<String> questionIds,
        List<String> exclusionPredicates,
        String reasonCode,
        String actionCode,
        List<String> sourceIds,
        String status,
        String requiresReleaseGate) {

    public boolean isEngineRule() {
        return "ENGINE".equals(conditionType);
    }

    public boolean appliesToStage(String stage) {
        return stages.contains(stage);
    }

    /** The predicate name of an ENGINE rule, or {@code null} for signal rules. */
    public String enginePredicate() {
        if (!isEngineRule() || condition == null) {
            return null;
        }
        JsonNode predicate = condition.get("predicate");
        return predicate == null ? null : predicate.asText(null);
    }
}
