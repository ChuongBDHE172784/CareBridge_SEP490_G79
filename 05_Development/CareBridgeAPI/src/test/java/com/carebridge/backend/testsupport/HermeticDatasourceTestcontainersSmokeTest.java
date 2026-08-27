package com.carebridge.backend.testsupport;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = HermeticDatasourceTestcontainersSmokeTest.HermeticDatasourceSmokeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
        "SUPABASE_DB_URL=jdbc:postgresql://live.invalid:5432/production",
        "SUPABASE_DB_USERNAME=poisoned-live-user",
        "SUPABASE_DB_PASSWORD=poisoned-live-password"
        })
@ActiveProfiles("hermetic")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class HermeticDatasourceTestcontainersSmokeTest {

    private static final String DATABASE_NAME = "carebridge_test";
    private static final String SCHEMA_NAME = "public";
    private static final List<String> SYSTEM_PROPERTY_KEYS = List.of(
            "spring.profiles.active",
            "carebridge.hermetic.datasource.url",
            "carebridge.hermetic.datasource.username",
            "carebridge.hermetic.datasource.password",
            "carebridge.hermetic.datasource.database-name",
            "carebridge.hermetic.datasource.schema");
    private static final Map<String, String> ORIGINAL_SYSTEM_PROPERTIES = captureSystemProperties();

    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES =
            PostgresTestImages.pg16()
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername("carebridge_test_user")
                    .withPassword("synthetic-test-password");

    static {
        POSTGRES.start();
        try {
            // The checklist migrations refuse to run unless deployment has already created
            // the NOLOGIN owner roles they hand objects to — Flyway itself has no CREATEROLE
            // dependency by design. Provision them exactly as AbstractPostgresIntegrationTest does.
            EmbeddedPostgresRoleFixture.provision(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not provision checklist database roles on the test container", exception);
        }
        System.setProperty("spring.profiles.active", "hermetic");
        System.setProperty("carebridge.hermetic.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("carebridge.hermetic.datasource.username", POSTGRES.getUsername());
        System.setProperty("carebridge.hermetic.datasource.password", POSTGRES.getPassword());
        System.setProperty("carebridge.hermetic.datasource.database-name", DATABASE_NAME);
        System.setProperty("carebridge.hermetic.datasource.schema", SCHEMA_NAME);
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private JdbcConnectionDetails connectionDetails;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void contextUsesOnlyNamedHermeticTestcontainer() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("hermetic");
        assertThat(environment.getPropertySources().contains("carebridgeDotenv")).isFalse();
        assertThat(environment.getProperty("carebridge.dotenv.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.flyway.default-schema")).isEqualTo(SCHEMA_NAME);
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo(POSTGRES.getJdbcUrl());
        assertThat(environment.getProperty("SUPABASE_DB_URL")).contains("live.invalid");

        assertThat(connectionDetails.getJdbcUrl()).isEqualTo(POSTGRES.getJdbcUrl());
        assertThat(connectionDetails.getUsername()).isEqualTo(POSTGRES.getUsername());

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(POSTGRES.getJdbcUrl());
            assertThat(connection.getCatalog()).isEqualTo(DATABASE_NAME);
            assertThat(connection.getSchema()).isEqualTo(SCHEMA_NAME);
        }

        try (Connection flywayConnection = flyway.getConfiguration().getDataSource().getConnection()) {
            assertThat(flywayConnection.getMetaData().getURL()).isEqualTo(POSTGRES.getJdbcUrl());
            assertThat(flywayConnection.getCatalog()).isEqualTo(DATABASE_NAME);
            try (var statement = flywayConnection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean(1)).isTrue();
            }
        }
        assertThat(flyway.getConfiguration().getDefaultSchema()).isEqualTo(SCHEMA_NAME);
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(entityManagerFactory.isOpen()).isTrue();
    }

    @AfterAll
    static void cleanup() {
        restoreSystemProperties();
        POSTGRES.stop();
    }

    private static Map<String, String> captureSystemProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : SYSTEM_PROPERTY_KEYS) {
            values.put(key, System.getProperty(key));
        }
        return values;
    }

    private static void restoreSystemProperties() {
        ORIGINAL_SYSTEM_PROPERTIES.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class HermeticDatasourceSmokeApplication {
    }
}
