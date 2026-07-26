package com.carebridge.backend.triage.dto;

import java.time.Instant;

/**
 * CB-TRIAGE-THMC-IMP-001 §8.1 — immutable advisory context value injected into the
 * AI payload and the Java fallback engine. Server-populated only (BR-THMC-006);
 * summaries are minimized and bounded (BR-THMC-003, ADR-THMC-003).
 *
 * @version 1.0
 */
public record HealthMemoryContextItem(
        String summaryText,   // minimized summary, truncated to maxSummaryChars
        String relatedStage,  // TriageStage name, e.g. "INFANT"
        Instant createdAt,
        Instant expiresAt     // nullable
) {}
