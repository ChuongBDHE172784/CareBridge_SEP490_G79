package com.carebridge.backend.checklist.model;

/**
 * Cadence declared by a checklist root version.
 *
 * <p>The value is persisted on {@code care_item_templates.schedule_type}; it is
 * deliberately separate from eligibility range units and recommendation copy.
 */
public enum ChecklistScheduleType {
    LEGACY,
    SET,
    WEEKLY,
    DAILY
}
