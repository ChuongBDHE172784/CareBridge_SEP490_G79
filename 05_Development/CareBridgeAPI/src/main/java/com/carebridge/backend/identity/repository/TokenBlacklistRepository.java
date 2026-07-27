package com.carebridge.backend.identity.repository;

import com.carebridge.backend.identity.entity.TokenBlacklist;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TokenBlacklistRepository {
    private final JdbcTemplate jdbcTemplate;

    public boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now) {
        Boolean found = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM auth_sessions
                     WHERE refresh_token_hash = ?
                       AND expires_at > ?
                       AND status = 'REVOKED'
                )
                """, Boolean.class, tokenHash, java.sql.Timestamp.from(now));
        return Boolean.TRUE.equals(found);
    }

    public TokenBlacklist save(TokenBlacklist revocation) {
        revocation.canonicalDefaults();
        // java.sql.Timestamp: the PostgreSQL driver cannot infer a SQL type for java.time.Instant.
        int updated = jdbcTemplate.update("""
                UPDATE auth_sessions
                   SET status = 'REVOKED',
                       revoked_at = ?,
                       revoke_reason = ?
                 WHERE refresh_token_hash = ?
                """, java.sql.Timestamp.from(revocation.getRevokedAt()),
                revocation.getReason(), revocation.getTokenHash());
        if (updated == 0) {
            throw new IllegalArgumentException("No auth session exists for the supplied token hash");
        }
        return revocation;
    }
}
