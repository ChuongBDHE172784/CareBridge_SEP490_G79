package com.carebridge.backend.common.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermeticDatasourceEnvironmentPostProcessorTest {

    private final HermeticDatasourceEnvironmentPostProcessor processor =
            new HermeticDatasourceEnvironmentPostProcessor();

    @Test
    void postProcessEnvironment_WhenDisabled_IsInert() {
        StandardEnvironment environment = new StandardEnvironment();

        assertThatCode(() -> process(environment)).doesNotThrowAnyException();
    }

    @Test
    void postProcessEnvironment_WithSafeHermeticConfiguration_AcceptsLoopback() {
        StandardEnvironment environment = hermeticEnvironment(Map.of());

        assertThatCode(() -> process(environment)).doesNotThrowAnyException();
    }

    @Test
    void postProcessEnvironment_WithPoisonedLegacyDatasource_AcceptsDedicatedLoopback() {
        StandardEnvironment environment = hermeticEnvironment(Map.of(
                "SUPABASE_DB_URL", "jdbc:postgresql://live.example.test:5432/postgres",
                "SUPABASE_DB_USERNAME", "live-user",
                "SUPABASE_DB_PASSWORD", "live-password"));

        assertThatCode(() -> process(environment)).doesNotThrowAnyException();
    }

    @Test
    void postProcessEnvironment_WithIpv6Loopback_AcceptsEndpoint() {
        String url = "jdbc:postgresql://[::1]:5432/carebridge_test";
        StandardEnvironment environment = hermeticEnvironment(Map.of(
                "carebridge.hermetic.datasource.url", url,
                "spring.datasource.url", url));

        assertThatCode(() -> process(environment)).doesNotThrowAnyException();
    }

    @Test
    void postProcessEnvironment_WithRemoteEndpoint_RejectsWithoutLeakingValue() {
        assertRejected(
                Map.of(
                        "carebridge.hermetic.datasource.url", "jdbc:postgresql://db.example.test:5432/live",
                        "spring.datasource.url", "jdbc:postgresql://db.example.test:5432/live"),
                "HERMETIC_DATASOURCE_REMOTE",
                "db.example.test");
    }

    @Test
    void postProcessEnvironment_WithCanonicalDatasourceOverride_RejectsMismatch() {
        assertRejected(
                Map.of("spring.datasource.url", "jdbc:postgresql://remote.example.test:5432/live"),
                "HERMETIC_DATASOURCE_MISMATCH",
                "remote.example.test");
    }

    @Test
    void postProcessEnvironment_WithCanonicalUsernameOverride_RejectsMismatch() {
        assertRejected(
                Map.of("spring.datasource.username", "poisoned-user"),
                "HERMETIC_USERNAME_MISMATCH",
                "poisoned-user");
    }

    @Test
    void postProcessEnvironment_WithCanonicalPasswordOverride_RejectsMismatch() {
        assertRejected(
                Map.of("spring.datasource.password", "poisoned-password"),
                "HERMETIC_PASSWORD_MISMATCH",
                "poisoned-password");
    }

    @Test
    void postProcessEnvironment_WithCredentialsInUrl_RejectsWithoutLeakingValue() {
        String url = "jdbc:postgresql://secret-user:secret-password@127.0.0.1:5432/carebridge";
        assertRejected(
                Map.of("carebridge.hermetic.datasource.url", url, "spring.datasource.url", url),
                "HERMETIC_DATASOURCE_CREDENTIALS",
                "secret-password");
    }

    @Test
    void postProcessEnvironment_WithMissingMandatoryProperty_Rejects() {
        assertRejected(
                Map.of("carebridge.hermetic.datasource.password", ""),
                "HERMETIC_DATASOURCE_MISSING",
                "password");
    }

    @Test
    void postProcessEnvironment_WithoutPortOrDatabaseName_Rejects() {
        String missingPort = "jdbc:postgresql://127.0.0.1/carebridge_test";
        assertRejected(
                Map.of(
                        "carebridge.hermetic.datasource.url", missingPort,
                        "spring.datasource.url", missingPort),
                "HERMETIC_DATASOURCE_MALFORMED",
                "carebridge");

        String missingDatabase = "jdbc:postgresql://127.0.0.1:5432/";
        assertRejected(
                Map.of(
                        "carebridge.hermetic.datasource.url", missingDatabase,
                        "spring.datasource.url", missingDatabase),
                "HERMETIC_DATASOURCE_MALFORMED",
                "127.0.0.1");
    }

    @Test
    void postProcessEnvironment_WithNonTestDatabaseName_Rejects() {
        String url = "jdbc:postgresql://127.0.0.1:5432/carebridge";
        assertRejected(
                Map.of(
                        "carebridge.hermetic.datasource.url", url,
                        "carebridge.hermetic.datasource.database-name", "carebridge",
                        "spring.datasource.url", url),
                "HERMETIC_DATABASE_NAME_UNSAFE",
                "carebridge");
    }

    @Test
    void postProcessEnvironment_WithMismatchedDatabaseName_Rejects() {
        assertRejected(
                Map.of("carebridge.hermetic.datasource.database-name", "other_test"),
                "HERMETIC_DATABASE_NAME_MISMATCH",
                "other_test");
    }

    @Test
    void postProcessEnvironment_WithUnknownNonTestSchema_Rejects() {
        assertRejected(
                Map.of(
                        "carebridge.hermetic.datasource.schema", "production",
                        "spring.datasource.hikari.schema", "production"),
                "HERMETIC_SCHEMA_UNSAFE",
                "production");
    }

    @Test
    void postProcessEnvironment_WithMismatchedSchema_Rejects() {
        assertRejected(
                Map.of("spring.datasource.hikari.schema", "other_test"),
                "HERMETIC_SCHEMA_MISMATCH",
                "other_test");
    }

    @Test
    void postProcessEnvironment_WithDotenvSourcePresent_Rejects() {
        StandardEnvironment environment = hermeticEnvironment(Map.of());
        environment.getPropertySources().addFirst(new MapPropertySource("carebridgeDotenv", Map.of()));

        assertThatThrownBy(() -> process(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HERMETIC_DOTENV_PRESENT");
    }

    @Test
    void postProcessEnvironment_WithoutHermeticProfile_Rejects() {
        assertRejected(
                Map.of("spring.profiles.active", "test"),
                "HERMETIC_PROFILE_REQUIRED",
                "test-password");
    }

    @Test
    void postProcessEnvironment_WithDotenvEnabled_Rejects() {
        assertRejected(
                Map.of("carebridge.dotenv.enabled", true),
                "HERMETIC_DOTENV_ENABLED",
                "test-password");
    }

    @Test
    void postProcessEnvironment_WithUnsafePersistenceSettings_Rejects() {
        assertRejected(Map.of("spring.jpa.hibernate.ddl-auto", "update"), "HERMETIC_DDL_MODE_UNSAFE", "update");
        assertRejected(Map.of("spring.flyway.enabled", false), "HERMETIC_FLYWAY_DISABLED", "false");
        assertRejected(Map.of("carebridge.dev-seed.enabled", true), "HERMETIC_DEV_SEED_ENABLED", "true");
    }

    private void assertRejected(Map<String, Object> overrides, String code, String secret) {
        StandardEnvironment environment = hermeticEnvironment(overrides);

        assertThatThrownBy(() -> process(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(code)
                .hasMessageNotContaining(secret);
    }

    private StandardEnvironment hermeticEnvironment(Map<String, Object> overrides) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.profiles.active", "hermetic");
        properties.put("carebridge.datasource-guard.enabled", true);
        properties.put("carebridge.dotenv.enabled", false);
        properties.put("carebridge.hermetic.datasource.url", "jdbc:postgresql://127.0.0.1:5432/carebridge_test");
        properties.put("carebridge.hermetic.datasource.username", "test-user");
        properties.put("carebridge.hermetic.datasource.password", "test-password");
        properties.put("carebridge.hermetic.datasource.database-name", "carebridge_test");
        properties.put("carebridge.hermetic.datasource.schema", "batch4_test");
        properties.put("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/carebridge_test");
        properties.put("spring.datasource.username", "test-user");
        properties.put("spring.datasource.password", "test-password");
        properties.put("spring.datasource.hikari.schema", "batch4_test");
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.flyway.enabled", true);
        properties.put("carebridge.dev-seed.enabled", false);
        properties.putAll(overrides);

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }

    private void process(StandardEnvironment environment) {
        processor.postProcessEnvironment(environment, new SpringApplication());
    }
}
