package com.carebridge.backend.security.policy;

import com.carebridge.backend.common.exception.AccountDisabledException;
import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.common.exception.AccountSuspendedException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Decides whether a user may authenticate.
 *
 * <p>This policy no longer consults the appeal workflow: locked accounts are
 * reported as locked, with the reason, and the user contacts customer support.
 * It therefore has no repository dependency and needs no database access to
 * answer, which is what the appeal-independence exit gate requires.
 */
@Component
public class AuthenticationPolicy {

    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60;

    public void ensureCanAuthenticate(User user) {
        if (user == null || !user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }
        if (user.isLocked()) {
            AccountLockType lockType = user.getLockType() == null
                    ? AccountLockType.TEMPORARY : user.getLockType();
            if (lockType == AccountLockType.TEMPORARY) {
                Instant retryAt = user.getLockedAt() == null
                        ? null : user.getLockedAt().plusSeconds(LOCKOUT_DURATION_SECONDS);
                if (retryAt != null && !Instant.now().isBefore(retryAt)) {
                    clearLock(user);
                    return;
                }
                throw new AccountLockedException(
                        "Account temporarily locked due to multiple failed attempts",
                        AccountLockType.TEMPORARY, null, retryAt);
            }
            throw new AccountLockedException(
                    "Account was locked by an administrator",
                    AccountLockType.ADMIN, user.getLockReason(), null);
        }
        if (user.getSuspendedUntil() != null && Instant.now().isBefore(user.getSuspendedUntil())) {
            throw new AccountSuspendedException("Account is suspended until " + user.getSuspendedUntil());
        }
    }

    public void applyTemporaryLock(User user, Instant lockedAt) {
        user.setLocked(true);
        user.setLockedAt(lockedAt);
        user.setLockType(AccountLockType.TEMPORARY);
        user.setLockReason(null);
        user.setLockedBy(null);
        user.setLockEpisodeId(null);
    }

    public void clearLock(User user) {
        user.setLocked(false);
        user.setLockedAt(null);
        user.setLockType(null);
        user.setLockReason(null);
        user.setLockedBy(null);
        user.setLockEpisodeId(null);
    }

    public Role resolveSelfRegistrationRole(Role requestedRole) {
        if (requestedRole == null) return null;
        if (requestedRole == Role.MOTHER || requestedRole == Role.FAMILY || requestedRole == Role.EXPERT) {
            return requestedRole;
        }
        throw new ValidationException("Role is not allowed for self-registration");
    }

    public void ensureOtpCanBeAttempted(OtpVerification verification, int maxAttempts) {
        if (verification.getAttempts() >= maxAttempts) {
            throw new ValidationException("OTP attempt limit exceeded");
        }
    }
}
