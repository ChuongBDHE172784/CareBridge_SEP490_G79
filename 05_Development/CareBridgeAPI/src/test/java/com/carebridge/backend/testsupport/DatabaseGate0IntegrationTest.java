package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class DatabaseGate0IntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Test
    void cleanBootstrapAppliesTheExpectedMigrationChain() throws Exception {
        var repository = DatabaseGate0Support.inspectRepository();
        List<BootstrapMigration> expected = repository.migrations().stream()
                .map(migration -> new BootstrapMigration(
                        migration.version(), migration.script(), migration.flywayChecksum()))
                .toList();
        List<BootstrapMigration> applied = new ArrayList<>();
        String migrationFailureType = null;
        String serverVersion = null;
        String runtimeImage = POSTGRES_IMAGE;

        try (PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)) {
            postgres.start();
            runtimeImage = postgres.getDockerImageName();
            try {
                Flyway.configure()
                        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .outOfOrder(true)
                        .load()
                        .migrate();
            } catch (RuntimeException exception) {
                migrationFailureType = exception.getClass().getSimpleName();
            }

            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var versionStatement = connection.createStatement();
                 var versionResult = versionStatement.executeQuery("SHOW server_version")) {
                versionResult.next();
                serverVersion = versionResult.getString(1);
                if (migrationFailureType == null) {
                    try (var historyStatement = connection.createStatement();
                         var historyResult = historyStatement.executeQuery("""
                                 SELECT version, script, checksum
                                   FROM flyway_schema_history
                                  WHERE success AND version IS NOT NULL AND type = 'SQL'
                                  ORDER BY installed_rank
                                 """)) {
                        while (historyResult.next()) {
                            applied.add(new BootstrapMigration(
                                    DatabaseGate0Support.canonicalVersion(historyResult.getString(1)),
                                    historyResult.getString(2),
                                    (Integer) historyResult.getObject(3)));
                        }
                    }
                }
            }
        } catch (RuntimeException exception) {
            migrationFailureType = exception.getClass().getSimpleName();
        }

        boolean passed = migrationFailureType == null
                && repository.gateFailures().isEmpty()
                && applied.equals(expected);
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put("status", passed ? "passed" : "failed");
        manifest.put("containerImage", runtimeImage);
        manifest.put("postgresVersion", serverVersion);
        manifest.put("migrationFailureType", migrationFailureType);
        manifest.put("expectedMigrationChain", expected);
        manifest.put("appliedMigrationChain", applied);
        manifest.put("repositoryGateFailures", repository.gateFailures());
        DatabaseGate0Support.writeManifest("clean-bootstrap-manifest.json", manifest);

        assertThat(migrationFailureType)
                .as("Clean bootstrap failure type")
                .isNull();
        assertThat(repository.gateFailures())
                .as("Gate 0 repository failure codes")
                .isEmpty();
        assertThat(applied).containsExactlyElementsOf(expected);
    }

    private record BootstrapMigration(String version, String script, Integer checksum) {
    }
}
