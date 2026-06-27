package com.carebridge.backend.common.exception;

/**
 * Exception thrown when a user account is disabled.
 * Maps to HTTP 403 Forbidden.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
