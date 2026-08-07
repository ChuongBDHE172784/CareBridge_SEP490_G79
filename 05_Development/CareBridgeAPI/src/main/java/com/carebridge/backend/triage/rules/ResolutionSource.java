package com.carebridge.backend.triage.rules;

/**
 * Where a resolved context value came from, highest precedence first. Recorded so an audit can
 * show whether a value was stated by the user or merely inferred — a stored profile must never
 * silently override what the user just typed.
 */
public enum ResolutionSource {
    EXPLICIT_CLARIFICATION_ANSWER,
    EXPLICIT_IN_LATEST_MESSAGE,
    EXPLICIT_SELECTED_PROFILE,
    CONFIRMED_CONVERSATION_TARGET,
    STAGE_SPECIFIC_CONTEXT,
    EXTRACTOR_INFERENCE,
    NONE;

    /** Lower ordinal wins. */
    public boolean outranks(ResolutionSource other) {
        return this.ordinal() < other.ordinal();
    }
}
