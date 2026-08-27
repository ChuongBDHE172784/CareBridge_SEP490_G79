package com.carebridge.backend.security.policy;

import com.carebridge.backend.common.exception.AccountDisabledException;
import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.common.exception.AccountSuspendedException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import com.carebridge.backend.identity.admin.entity.AccountLockAppeal;
import com.carebridge.backend.identity.admin.repository.AccountLockAppealRepository;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationPolicy {

    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60;
    private JwtTokenProvider jwtTokenProvider;
    private AccountLockAppealRepository appealRepository;

    public AuthenticationPolicy() {
    }

    @Autowired
    public AuthenticationPolicy(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired(required = false)
    public void setAppealRepository(AccountLockAppealRepository appealRepository) {
        this.appealRepository = appealRepository;
    }

    public void ensureCanAuthenticate(User user) {
        ensureCanAuthenticate(user, true);
    }

    /**
     * @param includeAppealToken true only after the caller has proved identity. JWT/refresh
     *                           enforcement must pass false to avoid issuing appeal credentials.
     */
    public void ensureCanAuthenticate(User user, boolean includeAppealToken) {
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
                        AccountLockType.TEMPORARY, null, retryAt, false, null);
            }
            AccountLockAppealStatus appealStatus = appealRepository == null || user.getLockEpisodeId() == null
                    ? null
                    : appealRepository.findTopByUserIdAndLockEpisodeIdOrderBySubmittedAtDesc(
                            user.getId(), user.getLockEpisodeId())
                            .map(AccountLockAppeal::getStatus)
                            .orElse(null);
            boolean hasAppealForEpisode = appealStatus != null;
            boolean hasPendingAppeal = appealStatus == AccountLockAppealStatus.PENDING;
            String appealToken = !hasAppealForEpisode && includeAppealToken && jwtTokenProvider != null
                    ? jwtTokenProvider.generateAppealToken(user) : null;
            throw new AccountLockedException(
                    "Account was locked by an administrator",
                    AccountLockType.ADMIN,
                    user.getLockReason(), null, !hasAppealForEpisode && includeAppealToken, appealToken,
                    hasPendingAppeal, appealStatus);
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
