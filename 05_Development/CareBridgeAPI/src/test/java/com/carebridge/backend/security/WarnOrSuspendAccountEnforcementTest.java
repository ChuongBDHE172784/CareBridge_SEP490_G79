package com.carebridge.backend.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.common.exception.AccountSuspendedException;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

// WSA-TC-220 — AuthenticationPolicy.ensureCanAuthenticate() login-gate enforcement (ADR-003 touchpoint #2)
class WarnOrSuspendAccountEnforcementTest {

    private final AuthenticationPolicy policy = new AuthenticationPolicy();

    @Test
    void ensureCanAuthenticate_suspendedUser_throwsAccountSuspendedException() {
        User suspendedUser = User.builder()
                .enabled(true)
                .locked(false)
                .suspendedUntil(Instant.now().plus(14, ChronoUnit.DAYS))
                .build();

        assertThatThrownBy(() -> policy.ensureCanAuthenticate(suspendedUser))
                .isInstanceOf(AccountSuspendedException.class);
    }
}
