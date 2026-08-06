package com.carebridge.backend.common.exception;

import com.carebridge.backend.security.entity.AccountLockType;
import java.time.Instant;

/**
 * Carries safe, post-credential account-lock context for client rendering.
 *
 * <p>The in-app appeal workflow was retired: locked users are directed to
 * customer support, which unlocks through the admin API. Nothing here exposes an
 * appeal token or appeal status any more.
 */
public class AccountLockedException extends RuntimeException {
    private final AccountLockType lockType;
    private final String reason;
    private final Instant retryAt;

    public AccountLockedException(String message) {
        this(message, AccountLockType.TEMPORARY, null, null);
    }

    public AccountLockedException(
            String message,
            AccountLockType lockType,
            String reason,
            Instant retryAt) {
        super(message);
        this.lockType = lockType;
        this.reason = reason;
        this.retryAt = retryAt;
    }

    public AccountLockType getLockType() { return lockType; }
    public String getReason() { return reason; }
    public Instant getRetryAt() { return retryAt; }
}
