package com.carebridge.backend.notification.entity;

public enum NotificationType {
    REMINDER,
    COMMUNITY_REPLY,
    CONSULTATION,
    EMERGENCY,
    LOCATION_SHARE,
    // ADR-MEDI-004 — direct-chat message alert (CB-EXPCHAT-IMP-001)
    MESSAGE,
    GROUP_INVITE,
    CONTENT_REVIEW,
    // CB-EPDS-IMP-001 — EPDS screening result delivered to consented care-group family members
    EPDS_RESULT
}
