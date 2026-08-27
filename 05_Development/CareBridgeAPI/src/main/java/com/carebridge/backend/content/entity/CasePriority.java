package com.carebridge.backend.content.entity;

/**
 * Review priority of a moderation case. User reports default to NORMAL; the AI moderation
 * decision policy raises priority for HIGH (HIGH) and CRITICAL (URGENT) matched policies.
 * Priority never triggers automatic enforcement — it only orders human review.
 */
public enum CasePriority {
    NORMAL, HIGH, URGENT
}
