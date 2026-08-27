package com.carebridge.backend.triage.rules;

/**
 * Who the triage session is about. Kept in lockstep with
 * {@code Contracts/triage/context_contract_v1.json}; a parity test asserts the values match.
 *
 * <p>Never modelled as {@code null}: an absent answer is {@link #UNKNOWN} and contradictory
 * evidence is {@link #CONFLICTED}, because a null would silently read as "no constraint" at
 * every call site. Exactly one primary target entity per session.
 */
public enum TargetEntity {
    MOTHER,
    BABY,
    UNKNOWN,
    /** Contradictory evidence, e.g. "tôi và bé đều bị sốt". Never resolved silently. */
    CONFLICTED;

    public boolean isResolved() {
        return this != UNKNOWN && this != CONFLICTED;
    }
}
