package com.carebridge.backend.common.exception;

import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import java.time.Instant;

/** Carries safe, post-credential account-lock context for client rendering. */
public class AccountLockedException extends RuntimeException {
    private final AccountLockType lockType;
    private final String reason;
    private final Instant retryAt;
    private final boolean appealAllowed;
    private final String appealToken;
    private final boolean appealPending;
    private final AccountLockAppealStatus appealStatus;

    public AccountLockedException(String message) {
        this(message, AccountLockType.TEMPORARY, null, null, false, null, false, null);
    }

    public AccountLockedException(
            String message,
            AccountLockType lockType,
            String reason,
            Instant retryAt,
            boolean appealAllowed,
            String appealToken) {
        this(message, lockType, reason, retryAt, appealAllowed, appealToken, false, null);
    }

    public AccountLockedException(
            String message,
            AccountLockType lockType,
            String reason,
            Instant retryAt,
            boolean appealAllowed,
            String appealToken,
            boolean appealPending) {
        this(message, lockType, reason, retryAt, appealAllowed, appealToken, appealPending, null);
    }

    public AccountLockedException(
            String message,
            AccountLockType lockType,
            String reason,
            Instant retryAt,
            boolean appealAllowed,
            String appealToken,
            boolean appealPending,
            AccountLockAppealStatus appealStatus) {
        super(message);
        this.lockType = lockType;
        this.reason = reason;
        this.retryAt = retryAt;
        this.appealAllowed = appealAllowed;
        this.appealToken = appealToken;
        this.appealPending = appealPending;
        this.appealStatus = appealStatus;
    }

    public AccountLockType getLockType() { return lockType; }
    public String getReason() { return reason; }
    public Instant getRetryAt() { return retryAt; }
    public boolean isAppealAllowed() { return appealAllowed; }
    public String getAppealToken() { return appealToken; }
    public boolean isAppealPending() { return appealPending; }
    public AccountLockAppealStatus getAppealStatus() { return appealStatus; }
}
