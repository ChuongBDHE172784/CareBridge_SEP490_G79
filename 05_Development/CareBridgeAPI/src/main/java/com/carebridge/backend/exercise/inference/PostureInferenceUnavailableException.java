package com.carebridge.backend.exercise.inference;

import lombok.Getter;

/**
 * Sanitized provider failure. The reason code is safe to persist or log; provider response
 * bodies and landmark payloads must never be included.
 */
@Getter
public class PostureInferenceUnavailableException extends RuntimeException {

    private final String reasonCode;

    public PostureInferenceUnavailableException(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }
}
