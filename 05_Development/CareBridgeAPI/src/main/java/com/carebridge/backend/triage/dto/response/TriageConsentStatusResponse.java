package com.carebridge.backend.triage.dto.response;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §8.1 — consent status for the Web/Mobile disclaimer dialog.
 */
@Getter
@Setter
@NoArgsConstructor
public class TriageConsentStatusResponse {

    /** {@code "REQUIRED"} | {@code "ACCEPTED"}. */
    private String status;

    /** When REQUIRED: {@code "NOT_ACCEPTED"} | {@code "VERSION_CHANGED"} | {@code "REVOKED"}; else null. */
    private String reason;

    /** {@code TriageDisclaimerPolicy.currentVersion()}. */
    private String currentVersion;

    /** {@code policy_version} of the effective ACTIVE row; null if none. */
    private String acceptedVersion;

    /** {@code granted_at} of that row; null if none. */
    private Instant acceptedAt;

    /** Canonical dialog text (configuration). */
    private String disclaimerText;
}
