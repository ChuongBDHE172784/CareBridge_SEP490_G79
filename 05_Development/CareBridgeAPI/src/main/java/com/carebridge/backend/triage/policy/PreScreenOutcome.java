package com.carebridge.backend.triage.policy;

/**
 * Outcome classes of the intake red-flag pre-screen (CB-TRIAGE-IMP-003, ADR-002).
 *
 * <ul>
 *   <li>{@link #ESCALATE_RED} — an active rule with severity RED and action ESCALATE or BLOCK
 *       matched; the intake session must short-circuit to RED without calling the AI (C1/C5).</li>
 *   <li>{@link #ANNOTATE_ONLY} — an active RED+WARN or YELLOW rule matched; the flow continues to
 *       the AI, the match is only annotated (metric/log + additive request context key).</li>
 *   <li>{@link #NO_MATCH} — no runtime-relevant rule matched (includes GREEN and inactive rules,
 *       blank input, and the degraded no-op state — see {@link PreScreenResult#degraded()}).</li>
 * </ul>
 *
 * @version 1.0
 */
public enum PreScreenOutcome {
    ESCALATE_RED,
    ANNOTATE_ONLY,
    NO_MATCH
}
