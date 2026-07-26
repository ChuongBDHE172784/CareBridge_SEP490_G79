package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.AcceptTriageConsentRequest;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.dto.response.TriageConsentStatusResponse;
import java.util.UUID;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §8.1 — AI-triage disclaimer consent contract.
 *
 * @version 1.0
 */
public interface ITriageConsentService {

    /** Current consent state for the dialog. Never throws domain errors for missing consent. */
    TriageConsentStatusResponse getStatus(UUID userId);

    /**
     * Idempotent acceptance of the CURRENT disclaimer version.
     *
     * @throws com.carebridge.backend.triage.exception.TriageException
     *         (409, TRIAGE_CONSENT_VERSION_MISMATCH) when {@code request.policyVersion}
     *         does not equal {@code TriageDisclaimerPolicy.currentVersion()}
     */
    TriageConsentAcceptOutcome accept(AcceptTriageConsentRequest request, UUID userId);

    /**
     * Revokes the newest ACTIVE consent row of this user.
     *
     * @throws com.carebridge.backend.triage.exception.TriageException
     *         (404, TRIAGE_CONSENT_NOT_FOUND) when no ACTIVE row exists
     */
    TriageConsentStatusResponse revoke(UUID userId);

    /**
     * GATE — called by {@code TriageService.runIntake()} and {@code startConversation()} ONLY
     * (BR-TDC-004 / BR-SAFETY: never wire this into continueConversation, continuations,
     * emergency escalation listeners, safety_events flows, or any UC62 path).
     *
     * @throws com.carebridge.backend.triage.exception.TriageException
     *         (409, TRIAGE_CONSENT_REQUIRED) when no ACTIVE row matches the current
     *         disclaimer version
     */
    void ensureActiveConsent(UUID userId);
}
