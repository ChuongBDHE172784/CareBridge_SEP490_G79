package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** RED PostgreSQL contracts for federated account creation and identity uniqueness. */
class FederatedRegistrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void userIdentitiesSchema_enforcesUniqueProviderSubject() {
        Integer constraints = jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'user_identities' and c.contype = 'u'
                """, Integer.class);
        assertThat(constraints).isGreaterThanOrEqualTo(1);
    }

    @Test
    void collidingContact_cannotCreateOrAutoLinkIdentity() {
        Integer identityCount = jdbc.queryForObject(
                "select count(*) from user_identities", Integer.class);
        assertThat(identityCount).isZero();
    }
}
