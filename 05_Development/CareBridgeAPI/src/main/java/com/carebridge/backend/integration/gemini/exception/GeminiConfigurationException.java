package com.carebridge.backend.integration.gemini.exception;

/**
 * Non-retryable Gemini failure: disabled feature, missing/invalid API key, unknown model,
 * malformed request, or a provider safety block on the given input. Messages and codes are
 * sanitized — they must never contain the API key or raw scanned content.
 */
public class GeminiConfigurationException extends RuntimeException {

    private final String errorCode;

    public GeminiConfigurationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
