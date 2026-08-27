package com.carebridge.backend.common.exception;

/**
 * Exception thrown when a user account is under a moderation-driven, time-bound suspension.
 * Maps to HTTP 403 Forbidden.
 */
public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}
