package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class CanonicalPhoneMigrationIntegrationTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void migrationCanonicalizesSeedPhonesAndCreatesPartialUniqueIndex() {
        Integer nonCanonical = jdbc.queryForObject("""
                select count(*)
                  from users
                 where phone is not null
                   and phone !~ '^\\+84[35789][0-9]{8}$'
                """, Integer.class);
        assertThat(nonCanonical).isZero();

        Integer indexCount = jdbc.queryForObject("""
                select count(*)
                  from pg_indexes
                 where schemaname = 'public'
                   and tablename = 'users'
                   and indexname = 'users_phone_canonical_uk'
                   and indexdef ilike '%unique%where (phone is not null)%'
                """, Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }

    @Test
    void migrationCanonicalizesLegacyOtpChallengeSubjects() {
        String challengePhone = jdbc.queryForObject("""
                select subject_identifier
                  from auth_challenges
                 where challenge_id = 'a0000000-0000-0000-0000-000000000001'
                """, String.class);

        assertThat(challengePhone).isEqualTo("+84901000004");
    }

    @Test
    void canonicalPhoneIndexRejectsDuplicateAuthenticationFactor() {
        String phone = jdbc.queryForObject(
                "select phone from users where phone is not null order by user_id limit 1", String.class);
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        assertThatThrownBy(() -> jdbc.update("""
                insert into users (
                    user_id, person_id, phone, created_at, updated_at,
                    enabled, locked, email_verified, phone_verified, settings_jsonb
                ) values (?, ?, ?, now(), now(), true, false, false, true, '{}'::jsonb)
                """, userId, personId, phone))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
