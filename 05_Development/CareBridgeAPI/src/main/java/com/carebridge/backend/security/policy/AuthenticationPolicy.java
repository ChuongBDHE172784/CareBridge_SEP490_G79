package com.carebridge.backend.security.policy;

import com.carebridge.backend.common.exception.AccountDisabledException;
import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.common.exception.AccountSuspendedException;

import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuthenticationPolicy {

    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60; // 15 minutes

    public void ensureCanAuthenticate(User user) {
        if (user == null || !user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }
        if (user.isLocked()) {
            // Check if lockout has expired (auto-unlock)
            if (user.getLockedAt() != null) {
                Instant lockExpiresAt = user.getLockedAt().plusSeconds(LOCKOUT_DURATION_SECONDS);
                if (Instant.now().isAfter(lockExpiresAt)) {
                    // Lock expired, auto-unlock the account by modifying the entity
                    user.setLocked(false);
                    user.setLockedAt(null);
                    return;
                }
            }
            throw new AccountLockedException("Account is locked");
        }
        // UC-102 ADR-003 touchpoint #2: block login for a moderation-driven, time-bound suspension
        if (user.getSuspendedUntil() != null && Instant.now().isBefore(user.getSuspendedUntil())) {
            throw new AccountSuspendedException("Account is suspended until " + user.getSuspendedUntil());
        }
    }

    public Role resolveSelfRegistrationRole(Role requestedRole) {
        Role role = requestedRole == null ? Role.MOTHER : requestedRole;
        if (role == Role.MOTHER || role == Role.FAMILY || role == Role.EXPERT) {
            return role;
        }
        throw new ValidationException("Role is not allowed for self-registration");
    }

    public void ensureOtpCanBeAttempted(OtpVerification verification, int maxAttempts) {
        if (verification.getAttempts() >= maxAttempts) {
            throw new ValidationException("OTP attempt limit exceeded");
        }
    }
}
