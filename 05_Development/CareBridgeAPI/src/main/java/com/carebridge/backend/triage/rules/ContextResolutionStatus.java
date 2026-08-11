package com.carebridge.backend.triage.rules;

/**
 * How much of the session context is settled. Kept in lockstep with
 * {@code Contracts/triage/context_contract_v1.json}.
 */
public enum ContextResolutionStatus {
    RESOLVED,
    NEEDS_TARGET_ENTITY,
    NEEDS_STAGE,
    NEEDS_INTENT,
    CONFLICTED,
    INSUFFICIENT_CONTEXT;

    /**
     * States in which the Question Planner may ask ONLY a clarification question. Asking a
     * symptom question before the target is known risks asking the mother about the baby, or
     * applying a maternal threshold to an infant.
     */
    public boolean blocksSymptomQuestions() {
        return this == NEEDS_TARGET_ENTITY
                || this == NEEDS_STAGE
                || this == NEEDS_INTENT
                || this == CONFLICTED
                || this == INSUFFICIENT_CONTEXT;
    }
}
