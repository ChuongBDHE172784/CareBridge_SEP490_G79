package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * A signal the approved rule set does not yet stratify.
 *
 * <p>A blocker asserts nothing clinical: it never names a disease and never raises a level.
 * It only prevents GREEN, because "no approved rule matched" is not evidence of low risk.
 */
public record TriageGreenBlocker(
        String blockerId,
        String title,
        List<String> stages,
        JsonNode condition,
        String predicate,
        String reasonCode,
        List<String> questionIds) {

    public boolean appliesToStage(String stage) {
        return stages.contains(stage);
    }
}
