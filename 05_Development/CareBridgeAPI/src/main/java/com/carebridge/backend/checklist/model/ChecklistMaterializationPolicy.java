package com.carebridge.backend.checklist.model;

/**
 * Occurrence materialization policy paired with a checklist root schedule.
 */
public enum ChecklistMaterializationPolicy {
    LEGACY_WINDOW,
    SEQUENCE_STEP,
    ONCE_PER_WINDOW,
    EACH_WEEK,
    EACH_DAY
}
