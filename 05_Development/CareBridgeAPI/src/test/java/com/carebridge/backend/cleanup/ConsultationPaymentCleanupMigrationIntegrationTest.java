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
class ConsultationPaymentCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE_CLEANUP =
            MigrationVersion.fromVersion("20260722020700");
    private static final MigrationVersion CONSULTATION_PAYMENT_CLEANUP =
            MigrationVersion.fromVersion("20260722020800");

    private static final String[] REMOVED_TABLES = {
            "consultation_requests",
            "consultation_messages",
            "consultation_disputes",
            "payment_transactions",
            "refund_records",
            "commission_config",
            "commission_records",
            "settlement_records",
            "expert_reviews"
    };

    private static final String[] BLOCKED_TABLES = {
            "consultation_bookings",
            "consultation_sessions",
            "consultation_price_bands",
            "expert_consultation_prices",
            "direct_conversations",
            "direct_messages",
            "conversation_calls",
            "partner_organizations"
    };

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
    }

    @Test
    void auditedEmptyWaveDropsOnlyApprovedTables() throws Exception {
        migrateTo(PRE_CLEANUP);

        migrateTo(CONSULTATION_PAYMENT_CLEANUP);

        for (String table : REMOVED_TABLES) {
            assertThat(relationExists(table)).as(table).isFalse();
        }
        for (String table : BLOCKED_TABLES) {
            assertThat(relationExists(table)).as(table).isTrue();
        }
        assertThat(relationExists("nearby_support_requests")).isTrue();
        assertThat(relationExists("nearby_support_responses")).isTrue();
        assertThat(relationExists("care_facilities")).isTrue();
        assertThat(relationExists("uq_notification_records_consultation_request")).isFalse();
    }

    @Test
    void nonzeroCandidateBlocksAndRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("""
                SET session_replication_role = replica;
                INSERT INTO commission_config (created_by) VALUES (gen_random_uuid());
                SET session_replication_role = origin
                """);

        assertThatThrownBy(() -> migrateTo(CONSULTATION_PAYMENT_CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("commission_config")
                .hasMessageContaining("1 row");

        assertCompleteRollback();
        assertThat(scalar("SELECT count(*) FROM commission_config")).isEqualTo("1");
    }

    @Test
    void unexpectedDependentViewBlocksAndRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("""
                CREATE MATERIALIZED VIEW retained_consultation_requests AS
                SELECT id FROM consultation_requests
                """);

        assertThatThrownBy(() -> migrateTo(CONSULTATION_PAYMENT_CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("consultation_requests")
                .hasMessageContaining("dependent view");

        assertCompleteRollback();
        assertThat(relationExists("retained_consultation_requests")).isTrue();
    }

    private void assertCompleteRollback() throws Exception {
        for (String table : REMOVED_TABLES) {
            assertThat(relationExists(table)).as(table).isTrue();
        }
        assertThat(relationExists("uq_notification_records_consultation_request")).isTrue();
    }

    private void migrateTo(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .target(target);
        configuration.load().migrate();
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean relationExists(String relation) throws Exception {
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
