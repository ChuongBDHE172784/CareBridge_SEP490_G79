package com.carebridge.backend.content.entity;

public enum ReportStatus {
    PENDING,
    // CB-MOD-IMP-016: claimed by a moderator (assigned_moderator_id + claimed_at set);
    // only PENDING -> IN_REVIEW -> RESOLVED/DISMISSED (or release back to PENDING) is legal.
    IN_REVIEW,
    RESOLVED,
    DISMISSED
}
