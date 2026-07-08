package com.carebridge.backend.common.exception;

/**
 * Exception thrown when a user account is temporarily locked due to failed login attempts.
 * Maps to HTTP 403 Forbidden.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
