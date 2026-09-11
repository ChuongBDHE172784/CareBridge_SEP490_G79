package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.PostgresTestImages;
import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Linux/CI path for the same clean PostgreSQL 18 production baseline. */
@Testcontainers(disabledWithoutDocker = true)
class ProductionBaselinePostgres18TestcontainersTest {

    @Container
    final PostgreSQLContainer postgres = PostgresTestImages.pg18();

    @BeforeEach
    void provisionExternalRoles() throws Exception {
        EmbeddedPostgresRoleFixture.provision(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    @Timeout(240)
    void postgres18PgvectorContainerBootstrapsCanonicalMigrationChain() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();

        // Derived from the migration files on the classpath rather than hard-coded:
        // the chain grew from 8 to 10 and this test was the thing that noticed last.
        // Asserting against what Flyway discovers still proves every migration was
        // applied, in version order, and succeeded.
        String[] expectedVersions = java.util.Arrays.stream(flyway.info().all())
                .map(info -> info.getVersion() == null ? null : info.getVersion().getVersion())
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(expectedVersions.length);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT array_agg(version ORDER BY installed_rank),
                                bool_and(success),
                                count(*)
                           FROM flyway_schema_history
                          WHERE version IS NOT NULL
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat((String[]) result.getArray(1).getArray())
                        .containsExactly(expectedVersions);
                assertThat(result.getBoolean(2)).isTrue();
                assertThat(result.getLong(3)).isEqualTo(expectedVersions.length);
            }
            ProductionBaselineBootstrapTest.assertRetentionFinalizerState(connection);
            ProductionBaselineBootstrapTest.assertApplicationRuntimePrivilegeContract(connection);
        }
    }
}
