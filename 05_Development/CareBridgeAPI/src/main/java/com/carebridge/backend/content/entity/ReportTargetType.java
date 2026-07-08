package com.carebridge.backend.content.entity;

/**
 * ACCOUNT is used only by {@link ModerationAction} (warn/suspend account actions, UC-101) —
 * the {@code moderation_actions.target_type} column has no CHECK constraint. EXPERT and USER
 * were added for UC-14 (Report Content or Account): the {@code content_reports.target_type}
 * CHECK constraint (chk_target_type, added in V2__spec_sync_from_tds.sql) already restricts
 * that column to QUESTION/ANSWER/CONTENT/EXPERT/USER — ACCOUNT is intentionally NOT valid for
 * new {@link ContentReport} rows.
 */
public enum ReportTargetType {
    QUESTION,
    ANSWER,
    CONTENT,
    ACCOUNT,
    EXPERT,
    USER
}
