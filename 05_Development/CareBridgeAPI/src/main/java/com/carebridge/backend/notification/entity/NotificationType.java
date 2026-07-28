package com.carebridge.backend.notification.entity;

public enum NotificationType {
    REMINDER,
    COMMUNITY_REPLY,
    CONSULTATION,
    EMERGENCY,
    // ADR-MEDI-004 — direct-chat message alert (CB-EXPCHAT-IMP-001)
    MESSAGE,
    GROUP_INVITE,
    CONTENT_REVIEW
}
