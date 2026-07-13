package com.carebridge.backend.content.entity;

public enum ModerationActionType {
    APPROVE,
    HIDE,
    LOCK,
    REQUEST_REVISION,
    /** A non-visibility-changing safety marker for moderator and admin review. */
    LABEL,
    WARN,
    SUSPEND,
    RESTRICT,
    // CB-MOD-IMP-009: only ever created by ModerationServiceImpl.undoModerationAction() — blocked
    // from the generic POST /actions endpoint (OUT_OF_SCOPE_ACTION_TYPES). No Flyway migration
    // needed: moderation_actions.action_type is a plain varchar(30), no DB CHECK constraint (ADR-005).
    UNDO
}
