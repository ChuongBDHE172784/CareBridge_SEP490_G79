package com.carebridge.backend.triage.dto.response;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §8.1 — service-layer accept result.
 * The controller maps {@code created} → HTTP 201 (new consent row) / 200 (idempotent no-op).
 */
public record TriageConsentAcceptOutcome(boolean created, TriageConsentStatusResponse status) {
}
