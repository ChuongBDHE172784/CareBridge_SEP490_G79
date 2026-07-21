package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalTriageMigrationIntegrationTest {

    private static final MigrationVersion PRE_BATCH_3 =
            MigrationVersion.fromVersion("20260722020400");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetDisposableSchema() throws SQLException {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
        }
    }

    @Test
    void preBatch3UpgradeDropsExactlyTheTwoEmptyLegacyTables() throws Exception {
        migrateTo(PRE_BATCH_3);
        int tableCountBefore = publicTableCount();
        UUID sessionId = insertCanonicalTriageRows();

        migrateTo(null);

        assertThat(publicTableCount()).isEqualTo(tableCountBefore - 2);
        assertThat(regclass("triage_answers")).isNull();
        assertThat(regclass("triage_assessments")).isNull();
        assertThat(regclass("intake_sessions")).isEqualTo("intake_sessions");
        assertThat(regclass("structured_intake_data")).isEqualTo("structured_intake_data");
        assertThat(count("intake_sessions", "id = '" + sessionId + "'::uuid")).isOne();
        assertThat(count("structured_intake_data", "session_id = '" + sessionId + "'::uuid")).isOne();
    }

    @Test
    void nonEmptyLegacyTableBlocksAndRollsBackTheWholeMigration() throws Exception {
        migrateTo(PRE_BATCH_3);
        UUID sessionId = insertCanonicalTriageRows();
        execute("""
                INSERT INTO triage_assessments (assessment_id, created_at)
                VALUES ('00000000-0000-0000-0000-000000000301', now())
                """);

        assertBlockedMigration();

        assertThat(regclass("triage_answers")).isEqualTo("triage_answers");
        assertThat(regclass("triage_assessments")).isEqualTo("triage_assessments");
        assertThat(count("triage_assessments", "TRUE")).isOne();
        assertThat(count("intake_sessions", "id = '" + sessionId + "'::uuid")).isOne();
        assertThat(count("structured_intake_data", "session_id = '" + sessionId + "'::uuid")).isOne();
    }

    @Test
    void externalCatalogDependencyBlocksAndPreservesBothLegacyTables() throws Exception {
        migrateTo(PRE_BATCH_3);
        execute("CREATE VIEW retained_triage_view AS SELECT assessment_id FROM triage_assessments");

        assertBlockedMigration();

        assertThat(regclass("triage_answers")).isEqualTo("triage_answers");
        assertThat(regclass("triage_assessments")).isEqualTo("triage_assessments");
        assertThat(regclass("retained_triage_view")).isEqualTo("retained_triage_view");
    }

    @Test
    void crossSchemaDynamicRoutineDependencyBlocksAndPreservesBothLegacyTables() throws Exception {
        migrateTo(PRE_BATCH_3);
        execute("CREATE SCHEMA reporting");
        execute("""
                CREATE FUNCTION reporting.legacy_triage_count() RETURNS bigint
                LANGUAGE plpgsql AS $$
                DECLARE legacy_count bigint;
                BEGIN
                    EXECUTE 'SELECT count(*) FROM triage_assessments' INTO legacy_count;
                    RETURN legacy_count;
                END
                $$
                """);

        assertBlockedMigration();

        assertThat(regclass("triage_answers")).isEqualTo("triage_answers");
        assertThat(regclass("triage_assessments")).isEqualTo("triage_assessments");
    }

    private void assertBlockedMigration() {
        assertThatThrownBy(() -> migrateTo(null))
                .isInstanceOf(FlywayException.class)
                .rootCause()
                .hasMessageContaining("BLOCKED_PARTIAL_TRIAGE_MIGRATION");
    }

    private void migrateTo(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private UUID insertCanonicalTriageRows() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        execute("""
                INSERT INTO users (user_id, created_at, updated_at, email, enabled, locked, role)
                VALUES ('00000000-0000-0000-0000-000000000010', now(), now(),
                        'batch3@carebridge.test', true, false, 'MOTHER')
                """);
        execute("""
                INSERT INTO intake_sessions (id, user_id, symptoms, status, created_by)
                VALUES ('%s', '00000000-0000-0000-0000-000000000010', 'masked', 'PROCESSING',
                        '00000000-0000-0000-0000-000000000010')
                """.formatted(sessionId));
        execute("""
                INSERT INTO structured_intake_data (session_id, symptom_list)
                VALUES ('%s', '[]'::jsonb)
                """.formatted(sessionId));
        return sessionId;
    }

    private int publicTableCount() throws SQLException {
        try (Connection connection = connection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT count(*)
                       FROM information_schema.tables
                      WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                     """)) {
            result.next();
            return result.getInt(1);
        }
    }

    private String regclass(String relation) throws SQLException {
        try (Connection connection = connection();
             var statement = connection.prepareStatement("SELECT to_regclass(?)::text")) {
            statement.setString(1, "public." + relation);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private long count(String table, String predicate) throws SQLException {
        try (Connection connection = connection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + table + " WHERE " + predicate)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
