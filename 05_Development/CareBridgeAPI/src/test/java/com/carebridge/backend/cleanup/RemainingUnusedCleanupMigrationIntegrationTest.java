package com.carebridge.backend.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class RemainingUnusedCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE_CLEANUP =
            MigrationVersion.fromVersion("20260722020900");
    private static final MigrationVersion CLEANUP =
            MigrationVersion.fromVersion("20260722021000");
    private static final String[] REMOVED = {
            "contribution_attachments",
            "expert_identity_verifications",
            "expert_verification_documents",
            "impact_assessment_ratings",
            "medical_contributions"
    };

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
    }

    @Test
    void cleanBootstrapAcceptsOnlyKnownLiveOnlyAbsences() throws Exception {
        migrateTo(PRE_CLEANUP);
        migrateTo(CLEANUP);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(exists("expert_credentials")).isTrue();
        assertThat(exists("contribution_points")).isTrue();
        assertThat(Integer.parseInt(scalar("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_type = 'BASE TABLE'
                """)))
                .as("clean-bootstrap public base-table count")
                .isEqualTo(99);
    }

    @Test
    void auditedLegacyShapeDropsKnownLiveOnlyTables() throws Exception {
        migrateTo(PRE_CLEANUP);
        installLiveOnlyEmptyFixture();
        migrateTo(CLEANUP);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
    }

    @Test
    void nonzeroCandidateRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("""
                SET session_replication_role = replica;
                INSERT INTO impact_assessment_ratings
                    (rating_id, user_id, content_id, rating_value, created_at, updated_at)
                VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 1, now(), now());
                SET session_replication_role = origin
                """);

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("impact_assessment_ratings");
        assertThat(exists("impact_assessment_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    @Test
    void dependentViewRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("CREATE VIEW retained_impact_ratings AS SELECT * FROM impact_assessment_ratings");

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("impact_assessment_ratings");
        assertThat(exists("retained_impact_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    private void installLiveOnlyEmptyFixture() throws Exception {
        execute("""
                CREATE TABLE medical_contributions (
                    medical_contribution_id uuid PRIMARY KEY
                );
                CREATE TABLE contribution_attachments (
                    contribution_attachment_id uuid PRIMARY KEY,
                    medical_contribution_id uuid REFERENCES medical_contributions(medical_contribution_id)
                );
                CREATE TABLE expert_identity_verifications (
                    expert_identity_verification_id uuid PRIMARY KEY
                )
                """);
    }

    private void migrateTo(MigrationVersion target) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .target(target)
                .load()
                .migrate();
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean exists(String relation) throws Exception {
        return "t".equals(scalar("SELECT to_regclass('public." + relation + "') IS NOT NULL"));
    }

    private String scalar(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
