package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** RED PostgreSQL contracts for federated session persistence and replay safety. */
class FederatedLoginIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void federatedLogin_persistsOnlyHashedRefreshReferences() {
        Integer hashedSessions = jdbc.queryForObject("""
                select count(*) from user_sessions
                where refresh_token_hash is not null and length(refresh_token_hash) = 64
                """, Integer.class);
        assertThat(hashedSessions).isPositive();
    }

    @Test
    void replayedProviderSubject_doesNotDuplicateIdentity() {
        Integer duplicateSubjects = jdbc.queryForObject("""
                select count(*) from (
                    select provider, provider_subject from user_identities
                    group by provider, provider_subject having count(*) > 1
                ) duplicates
                """, Integer.class);
        assertThat(duplicateSubjects).isZero();
    }
}
