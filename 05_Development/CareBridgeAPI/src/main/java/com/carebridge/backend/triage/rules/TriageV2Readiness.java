package com.carebridge.backend.triage.rules;

/**
 * Why Triage V2 is or is not usable.
 *
 * <p>Deliberately separate from application liveness. A broken triage rule set must not take
 * the whole backend down — unrelated modules keep serving — but it must also never be
 * papered over, so the reason is captured here instead of being logged and forgotten.
 */
public enum TriageV2Readiness {
    READY,
    DISABLED_BY_FEATURE_FLAG,
    REGISTRY_INVALID,
    RULESET_HASH_MISMATCH,
    CRITICAL_RULE_MISSING,
    SOURCE_VERIFICATION_PENDING,
    INTERNAL_REVIEW_REQUIRED,
    PROVISIONAL_RENDERER,
    NOT_CLINICALLY_VALIDATED,
    FALLBACK_ONLY;

    /** Technical usability of the V2 engine, independent of release or validation policy. */
    public enum TechnicalStatus { READY, UNAVAILABLE, FALLBACK_ONLY }

    /** Whether the module may face real users. Never derived from technical status alone. */
    public enum PublicReleaseStatus { BLOCKED, INTERNAL_ONLY }

    public enum GreenReleaseStatus {
        DISABLED,
        INTERNAL_TEST_ONLY,
        BLOCKED_BY_SOURCE_COVERAGE,
        BLOCKED_BY_RULE_COVERAGE,
        BLOCKED_BY_DATASET,
        BLOCKED_BY_EVALUATION,
        ENABLED
    }
}
