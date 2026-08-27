package com.carebridge.backend.aimoderation.exception;

/**
 * The model returned JSON that does not satisfy the moderation schema (malformed, wrong
 * enums, out-of-range confidence). Treated as a RETRYABLE scan failure — never as SAFE.
 */
public class AiVerdictParseException extends RuntimeException {

    public AiVerdictParseException(String message) {
        super(message);
    }

    public AiVerdictParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
