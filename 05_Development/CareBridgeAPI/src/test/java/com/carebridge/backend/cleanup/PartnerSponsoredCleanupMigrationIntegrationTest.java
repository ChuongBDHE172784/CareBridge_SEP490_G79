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
class PartnerSponsoredCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE_CLEANUP =
            MigrationVersion.fromVersion("20260722020800");
    private static final MigrationVersion CLEANUP =
            MigrationVersion.fromVersion("20260722020900");
    private static final String[] REMOVED = {
            "partner_expert_links", "partner_services", "sponsored_campaigns"
    };

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
    }

    @Test
    void dropsOnlyAuditedEmptyPartnerChildren() throws Exception {
        migrateTo(PRE_CLEANUP);
        migrateTo(CLEANUP);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(exists("partner_organizations")).isTrue();
        assertThat(exists("care_facilities")).isTrue();
    }

    @Test
    void nonzeroChildBlocksCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("""
                SET session_replication_role = replica;
                INSERT INTO partner_expert_links
                    (partner_expert_link_id, partner_id, expert_profile_id, status, created_at)
                VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 'ACTIVE', now());
                SET session_replication_role = origin
                """);

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("partner_expert_links");
        assertAllPresent();
    }

    @Test
    void dependentViewBlocksCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("CREATE VIEW retained_partner_services AS SELECT * FROM partner_services");

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("partner_services");
        assertAllPresent();
        assertThat(exists("retained_partner_services")).isTrue();
    }

    private void assertAllPresent() throws Exception {
        for (String table : REMOVED) assertThat(exists(table)).as(table).isTrue();
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
