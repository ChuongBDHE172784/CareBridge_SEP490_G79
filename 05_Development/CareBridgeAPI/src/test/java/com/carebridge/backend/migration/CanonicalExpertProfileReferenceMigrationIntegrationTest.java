package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalExpertProfileReferenceMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260724214100");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260724214200");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE");
        execute("CREATE SCHEMA public");
    }

    @Test
    void populatedUpgradeRetiresThreeMatchingLegacyReferences() throws Exception {
        migrate(PRE);
        execute("""
                INSERT INTO persons (person_id, display_name, created_at, updated_at)
                VALUES ('42100000-0000-0000-0000-000000000001',
                        'Canonical expert reference', now(), now());
                INSERT INTO users (
                    user_id, person_id, email, role, enabled, locked, created_at, updated_at)
                VALUES ('42100000-0000-0000-0000-000000000001',
                        '42100000-0000-0000-0000-000000000001',
                        'canonical-expert-reference@test.invalid', 'EXPERT',
                        true, false, now(), now());
                INSERT INTO professional_profiles (
                    professional_profile_id, user_id, specialty, verification_status,
                    trust_status, created_at, updated_at)
                VALUES ('42200000-0000-0000-0000-000000000001',
                        '42100000-0000-0000-0000-000000000001',
                        'OBSTETRICS', 'APPROVED', 'ACTIVE', now(), now());
                INSERT INTO expert_credentials (
                    credential_id, expert_profile_id, professional_profile_id,
                    credential_type, review_status, created_at, updated_at)
                VALUES ('42300000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        'MEDICAL_LICENSE', 'APPROVED', now(), now());
                INSERT INTO expert_availability (
                    availability_id, expert_profile_id, professional_profile_id,
                    start_at, end_at, channel_type, status, created_at, updated_at)
                VALUES ('42400000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        now() + interval '1 hour', now() + interval '2 hours',
                        'VIDEO', 'AVAILABLE', now(), now());
                INSERT INTO expert_location_shares (
                    location_share_id, expert_profile_id, professional_profile_id,
                    latitude, longitude, availability_status, shared_at, expires_at,
                    created_at, updated_at)
                VALUES ('42500000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        '42200000-0000-0000-0000-000000000001',
                        10.7769, 106.7009, 'OFFLINE', now(),
                        now() + interval '30 minutes', now(), now());
                """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM expert_credentials WHERE professional_profile_id="
                + "'42200000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM expert_availability WHERE professional_profile_id="
                + "'42200000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM expert_location_shares WHERE professional_profile_id="
                + "'42200000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("""
                SELECT count(*)
                  FROM information_schema.columns
                 WHERE table_schema='public'
                   AND table_name IN (
                       'expert_credentials',
                       'expert_availability',
                       'expert_location_shares')
                   AND column_name='expert_profile_id'
                """)).isZero();
        assertThat(number("""
                SELECT count(*)
                  FROM pg_constraint
                 WHERE conname IN (
                       'expert_credentials_professional_profile_id_fkey',
                       'expert_availability_professional_profile_id_fkey',
                       'expert_location_shares_professional_profile_id_fkey')
                   AND contype='f'
                   AND convalidated
                   AND confdeltype IN ('a', 'r')
                """)).isEqualTo(3);
    }

    @Test
    void alternateNameCascadeForeignKeyFailsClosedBeforeLegacyColumnRemoval() throws Exception {
        migrate(PRE);
        execute("""
                ALTER TABLE expert_credentials
                    ADD CONSTRAINT expert_credentials_cascade_bypass_fkey
                    FOREIGN KEY (professional_profile_id)
                    REFERENCES professional_profiles(professional_profile_id)
                    ON DELETE CASCADE
                    NOT VALID;
                """);

        assertThatThrownBy(() -> migrate(WAVE))
                .hasStackTraceContaining(
                        "CANONICAL_EXPERT_REFERENCE: expert_credentials contains an unsafe "
                                + "canonical foreign key delete action");

        assertThat(number("""
                SELECT count(*)
                  FROM information_schema.columns
                 WHERE table_schema='public'
                   AND table_name='expert_credentials'
                   AND column_name='expert_profile_id'
                """)).isOne();
        assertThat(number("""
                SELECT count(*)
                  FROM pg_constraint
                 WHERE conrelid='public.expert_credentials'::regclass
                   AND conname='expert_credentials_cascade_bypass_fkey'
                   AND confdeltype='c'
                """)).isOne();
    }

    private void migrate(MigrationVersion target) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private long number(String sql) throws Exception {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
