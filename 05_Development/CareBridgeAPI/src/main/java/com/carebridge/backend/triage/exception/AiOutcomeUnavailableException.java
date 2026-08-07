package com.carebridge.backend.triage.exception;

/**
 * Raised when an AI-assisted path cannot produce a result.
 *
 * <p>This type exists to close a fail-open hole. The Gemini triage adapter and the dev
 * stubs used to return {@code RiskLevel.GREEN} when the model was unavailable or its
 * response was unparseable — that is, an outage produced the most reassuring possible
 * answer. There is no "cannot determine" value in {@link com.carebridge.backend.triage.RiskLevel},
 * so the only safe signal is to fail loudly and let the caller degrade deliberately
 * (NEEDS_MORE_INFO), never silently.
 *
 * <p>Callers must map this to NEEDS_MORE_INFO. They must never map it to GREEN.
 */
public class AiOutcomeUnavailableException extends RuntimeException {

    public AiOutcomeUnavailableException(String message) {
        super(message);
    }
}
