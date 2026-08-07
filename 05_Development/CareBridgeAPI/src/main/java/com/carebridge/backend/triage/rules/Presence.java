package com.carebridge.backend.triage.rules;

import java.util.EnumSet;
import java.util.Set;

/**
 * How much we actually know about one clinical signal. Parity with
 * {@code app/rules/condition.py::Presence}.
 */
public enum Presence {
    /** User confirmed, or a measurement crossed a defined threshold. */
    PRESENT,
    /** User explicitly denied it. This is a real negative. */
    ABSENT,
    /** Not asked, or answered "not sure". */
    UNKNOWN,
    /** Contradictory answers across turns. */
    CONFLICTED,
    /**
     * The user cannot answer at all — no blood-pressure cuff, no thermometer. Logically
     * identical to UNKNOWN, but kept distinct so the planner stops re-asking for a number
     * and pivots to symptoms the person can perceive.
     */
    UNAWARE_OR_UNMEASURABLE;

    /** Values that carry no answer and must never be read as a denial. */
    public static final Set<Presence> UNRESOLVED =
            EnumSet.of(UNKNOWN, CONFLICTED, UNAWARE_OR_UNMEASURABLE);

    public boolean isUnresolved() {
        return UNRESOLVED.contains(this);
    }

    /** Lenient parse: absent value, unknown text and non-enum input all read as UNKNOWN. */
    public static Presence parse(Object raw) {
        if (raw == null) return UNKNOWN;
        if (raw instanceof Presence presence) return presence;
        if (raw instanceof Boolean flag) return flag ? PRESENT : ABSENT;
        if (raw instanceof String text) {
            try {
                return Presence.valueOf(text);
            } catch (IllegalArgumentException ignored) {
                return UNKNOWN;
            }
        }
        return UNKNOWN;
    }
}
