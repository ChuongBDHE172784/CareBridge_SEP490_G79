package com.carebridge.backend.journey.entity;

public enum JourneyTransitionType {
    CREATED,
    STAGE_CHANGED,
    DATES_CHANGED,
    /** A resolved dating anchor was corrected; cadence must supersede prior work. */
    DATING_CORRECTED,
    DETAILS_CHANGED,
    STATUS_CHANGED,
    OUTCOME_RECORDED,
    OUTCOME_CORRECTED,
    /**
     * Server-inferred pregnancy dating authority event written by the checklist
     * P2 backfill.  It is a real canonical Journey event, not a generic audit
     * category, so the immutable transition projection must be able to hydrate it.
     */
    PREGNANCY_EPOCH_STARTED,
    MIGRATED
}
