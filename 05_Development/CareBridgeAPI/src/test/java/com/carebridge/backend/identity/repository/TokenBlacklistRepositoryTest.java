package com.carebridge.backend.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.identity.entity.TokenBlacklist;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TokenBlacklistRepositoryTest {

    @Test
    void revocationUpdatesExistingSessionAndNeverInsertsDuplicate() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(), any(), any())).thenReturn(1);
        var repository = new TokenBlacklistRepository(jdbc);
        TokenBlacklist revocation = TokenBlacklist.builder()
                .tokenHash("refresh-hash")
                .expiresAt(Instant.now().plusSeconds(60))
                .reason("logout")
                .build();

        assertThat(repository.save(revocation).getStatus()).isEqualTo("REVOKED");
        verify(jdbc).update(contains("UPDATE auth_sessions"), any(), eq("logout"), eq("refresh-hash"));
    }
}
