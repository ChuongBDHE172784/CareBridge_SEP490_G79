package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * A safety behaviour that is NOT part of the clinical Rule Matrix.
 *
 * <p>These exist because dropping them would regress safety versus V1 (cyanosis, self-harm
 * escalation), but they have no clinical validation as triage rules. They are a separate
 * type on purpose: nothing in the loader can promote a policy into {@link TriageRule}, and
 * {@code TEMPORARY_SAFETY_HOLDOVER} must never be read as clinical approval.
 */
public record TriageSafetyPolicy(
        String policyId,
        String policyType,
        String title,
        List<String> stages,
        String outcome,
        int priority,
        int decisionOrder,
        boolean stopConversation,
        JsonNode condition,
        String reasonCode,
        String actionCode,
        String status,
        boolean enabled,
        String reviewDueAt,
        String expiresAt) {

    public boolean appliesToStage(String stage) {
        return stages.contains(stage);
    }
}
