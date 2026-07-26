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
    /** Moderator recommendation retained for System Admin follow-up; no account mutation. */
    ESCALATE,
    // CB-MOD-IMP-009: only ever created by ModerationServiceImpl.undoModerationAction() — blocked
    // from the generic POST /actions endpoint (OUT_OF_SCOPE_ACTION_TYPES). No Flyway migration
    // moderation_events.action_type remains an extensible varchar (ADR-005).
    UNDO,
    /**
     * CB-MOD-IMP-017: append-only history of moderator agree/disagree feedback on an AI
     * assessment (payload in event_payload_jsonb). Excluded from moderation history views and
     * from the "most recent action" guards of undo/revert — it never mutates content state.
     */
    AI_FEEDBACK_SUBMITTED
}
