package com.carebridge.backend.security.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.identity.admin.entity.AccountLockAppeal;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import com.carebridge.backend.identity.admin.repository.AccountLockAppealRepository;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationPolicyAccountLockAppealTest {

    @Test
    void ensureCanAuthenticate_rejectedAppealReportsRejectionWithoutIssuingAnotherAppealToken() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AccountLockAppealRepository appealRepository = mock(AccountLockAppealRepository.class);
        AuthenticationPolicy policy = new AuthenticationPolicy(jwtTokenProvider);
        policy.setAppealRepository(appealRepository);
        UUID episodeId = UUID.randomUUID();
        User user = User.builder()
                .id(UUID.randomUUID())
                .enabled(true)
                .locked(true)
                .lockType(AccountLockType.ADMIN)
                .lockEpisodeId(episodeId)
                .build();
        AccountLockAppeal appeal = AccountLockAppeal.builder()
                .userId(user.getId())
                .lockEpisodeId(episodeId)
                .status(AccountLockAppealStatus.REJECTED)
                .submittedAt(Instant.now())
                .build();
        when(appealRepository.findTopByUserIdAndLockEpisodeIdOrderBySubmittedAtDesc(user.getId(), episodeId))
                .thenReturn(Optional.of(appeal));

        assertThatThrownBy(() -> policy.ensureCanAuthenticate(user))
                .isInstanceOfSatisfying(AccountLockedException.class, exception -> {
                    assertThat(exception.isAppealAllowed()).isFalse();
                    assertThat(exception.isAppealPending()).isFalse();
                    assertThat(exception.getAppealStatus()).isEqualTo(AccountLockAppealStatus.REJECTED);
                    assertThat(exception.getAppealToken()).isNull();
                });
    }
}

