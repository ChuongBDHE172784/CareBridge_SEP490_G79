package com.carebridge.backend.triage.rules;

/**
 * Kleene three-valued logic, kept in exact parity with {@code app/rules/condition.py::Tri}.
 *
 * <p>Data the user has not given is UNKNOWN, not FALSE. Collapsing the two would let
 * {@code not(missing)} evaluate to true, so an unanswered question could "prove" a symptom
 * is absent. These tables make that impossible.
 */
public enum Tri {
    TRUE,
    FALSE,
    UNKNOWN;

    public Tri and(Tri other) {
        if (this == FALSE || other == FALSE) return FALSE;
        if (this == UNKNOWN || other == UNKNOWN) return UNKNOWN;
        return TRUE;
    }

    public Tri or(Tri other) {
        if (this == TRUE || other == TRUE) return TRUE;
        if (this == UNKNOWN || other == UNKNOWN) return UNKNOWN;
        return FALSE;
    }

    public Tri negate() {
        if (this == UNKNOWN) return UNKNOWN;
        return this == TRUE ? FALSE : TRUE;
    }

    public static Tri of(boolean value) {
        return value ? TRUE : FALSE;
    }
}
