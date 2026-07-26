package com.carebridge.backend.triage.policy;

import java.util.List;
import java.util.UUID;

/**
 * Immutable result of {@link TriageRedFlagPreScreenPolicy#screen(String)} (CB-TRIAGE-IMP-003 §8.1).
 *
 * <p>{@code degraded=true} means the rule lookup failed and the pre-screen intentionally no-ops
 * (ADR-003 / BR-SAFETY-TRFP-002); {@code outcome} is then always {@link PreScreenOutcome#NO_MATCH}.
 *
 * <p>{@code matchedKeywords} carry the original (un-normalized) keywords of the matched rules.
 *
 * <p>Note: the TDS class diagram names the degraded static factory {@code degraded()}, but a Java
 * record component named {@code degraded} already claims that zero-arg signature for its accessor;
 * the factory is therefore named {@link #degradedNoMatch()} (documented deviation).
 *
 * @version 1.0
 */
public record PreScreenResult(
        PreScreenOutcome outcome,
        List<String> matchedKeywords,
        List<UUID> matchedRuleIds,
        boolean degraded) {

    public PreScreenResult {
        matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
        matchedRuleIds = matchedRuleIds == null ? List.of() : List.copyOf(matchedRuleIds);
    }

    /** No rule matched; pre-screen healthy. */
    public static PreScreenResult noMatch() {
        return new PreScreenResult(PreScreenOutcome.NO_MATCH, List.of(), List.of(), false);
    }

    /** Rule lookup failed; pre-screen degrades to a no-op (ADR-003). */
    public static PreScreenResult degradedNoMatch() {
        return new PreScreenResult(PreScreenOutcome.NO_MATCH, List.of(), List.of(), true);
    }
}
