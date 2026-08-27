package com.carebridge.backend.reminder.entity;

public enum ReminderType {
    APPOINTMENT,
    MEDICATION,
    VACCINATION,
    /** CB-TYFU-IMP-001 ADR-TYFU-001 — automatic follow-up after a YELLOW triage session. */
    TRIAGE_FOLLOW_UP
}
