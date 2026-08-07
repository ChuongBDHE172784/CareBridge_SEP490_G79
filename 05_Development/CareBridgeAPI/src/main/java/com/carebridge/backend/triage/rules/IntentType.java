package com.carebridge.backend.triage.rules;

/**
 * What the user is asking for. Kept in lockstep with
 * {@code Contracts/triage/context_contract_v1.json}.
 *
 * <p>Only {@link #SYMPTOM_TRIAGE} and {@link #FOLLOW_UP_ANSWER} may produce a triage colour.
 * Asking what a danger sign <em>is</em> must never be answered as an assessment of the user.
 * {@link #EMERGENCY_HELP} does not itself decide RED — the Global Safety Gate does.
 */
public enum IntentType {
    SYMPTOM_TRIAGE,
    GENERAL_HEALTH_INFORMATION,
    SOURCE_LOOKUP,
    FOLLOW_UP_ANSWER,
    EMERGENCY_HELP,
    OUT_OF_SCOPE_REQUEST,
    UNKNOWN,
    CONFLICTED;

    public boolean mayProduceTriageOutcome() {
        return this == SYMPTOM_TRIAGE || this == FOLLOW_UP_ANSWER;
    }

    public boolean isResolved() {
        return this != UNKNOWN && this != CONFLICTED;
    }
}
