package com.carebridge.backend.integration.gemini.exception;

public class GeminiUnavailableException extends RuntimeException {

    public GeminiUnavailableException(String message) {
        super(message);
    }

    public GeminiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
