package com.carebridge.backend.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public class HermeticDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String EXPECTED_URL = "carebridge.hermetic.datasource.url";
    private static final String EXPECTED_USERNAME = "carebridge.hermetic.datasource.username";
    private static final String EXPECTED_PASSWORD = "carebridge.hermetic.datasource.password";
    private static final String EXPECTED_DATABASE_NAME = "carebridge.hermetic.datasource.database-name";
    private static final String EXPECTED_SCHEMA = "carebridge.hermetic.datasource.schema";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("carebridge.datasource-guard.enabled", Boolean.class, false)) {
            return;
        }
        if (!environment.acceptsProfiles(Profiles.of("hermetic"))) {
            fail("HERMETIC_PROFILE_REQUIRED");
        }
        if (environment.getProperty("carebridge.dotenv.enabled", Boolean.class, true)) {
            fail("HERMETIC_DOTENV_ENABLED");
        }
        if (environment.getPropertySources().contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)) {
            fail("HERMETIC_DOTENV_PRESENT");
        }

        String expectedUrl = required(environment, EXPECTED_URL);
        String expectedUsername = required(environment, EXPECTED_USERNAME);
        String expectedPassword = required(environment, EXPECTED_PASSWORD);
        String expectedDatabaseName = required(environment, EXPECTED_DATABASE_NAME);
        String expectedSchema = required(environment, EXPECTED_SCHEMA);
        String effectiveUrl = requireMatching(
                environment, "spring.datasource.url", expectedUrl, "HERMETIC_DATASOURCE_MISMATCH");
        requireMatching(
                environment, "spring.datasource.username", expectedUsername, "HERMETIC_USERNAME_MISMATCH");
        requireMatching(
                environment, "spring.datasource.password", expectedPassword, "HERMETIC_PASSWORD_MISMATCH");

        validateJdbcUrl(effectiveUrl, expectedDatabaseName);
        validateHermeticSchema(expectedSchema);
        requireMatching(
                environment, "spring.datasource.hikari.schema", expectedSchema, "HERMETIC_SCHEMA_MISMATCH");
        requireValue(environment, "spring.jpa.hibernate.ddl-auto", "validate", "HERMETIC_DDL_MODE_UNSAFE");
        if (!environment.getProperty("spring.flyway.enabled", Boolean.class, false)) {
            fail("HERMETIC_FLYWAY_DISABLED");
        }
        if (environment.getProperty("carebridge.dev-seed.enabled", Boolean.class, false)) {
            fail("HERMETIC_DEV_SEED_ENABLED");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static String required(ConfigurableEnvironment environment, String key) {
        try {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                fail("HERMETIC_DATASOURCE_MISSING");
            }
            return value;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("HERMETIC_DATASOURCE_MISSING");
        }
    }

    private static void validateJdbcUrl(String jdbcUrl, String expectedDatabaseName) {
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            fail("HERMETIC_DATASOURCE_MALFORMED");
        }

        URI uri;
        try {
            uri = new URI(jdbcUrl.substring("jdbc:".length()));
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("HERMETIC_DATASOURCE_MALFORMED");
        }
        if (uri.getUserInfo() != null || containsCredentialQueryParameter(uri.getRawQuery())) {
            fail("HERMETIC_DATASOURCE_CREDENTIALS");
        }

        String host = uri.getHost();
        String databasePath = uri.getPath();
        if (host == null || host.isBlank()
                || uri.getPort() <= 0
                || databasePath == null
                || databasePath.isBlank()
                || "/".equals(databasePath)) {
            fail("HERMETIC_DATASOURCE_MALFORMED");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        if (!LOOPBACK_HOSTS.contains(normalizedHost)) {
            fail("HERMETIC_DATASOURCE_REMOTE");
        }

        String databaseName = databasePath.substring(1);
        if (databaseName.contains("/") || !databaseName.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
            fail("HERMETIC_DATABASE_NAME_UNSAFE");
        }
        if (!expectedDatabaseName.equals(databaseName)) {
            fail("HERMETIC_DATABASE_NAME_MISMATCH");
        }
        validateTestIdentifier(databaseName, "HERMETIC_DATABASE_NAME_UNSAFE");
    }

    private static void validateTestIdentifier(String value, String failureCode) {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_-]*")
                || !value.matches("(?i)(?:test(?:[_-].*)?|.*[_-]test(?:[_-].*)?)")) {
            fail(failureCode);
        }
    }

    private static void validateHermeticSchema(String value) {
        if ("public".equals(value)) {
            return;
        }
        validateTestIdentifier(value, "HERMETIC_SCHEMA_UNSAFE");
    }

    private static boolean containsCredentialQueryParameter(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return false;
        }
        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2)[0].toLowerCase(Locale.ROOT))
                .anyMatch(key -> key.equals("user") || key.equals("username") || key.equals("password"));
    }

    private static void requireValue(
            ConfigurableEnvironment environment,
            String key,
            String requiredValue,
            String failureCode) {
        String value = environment.getProperty(key);
        if (value == null || !requiredValue.equalsIgnoreCase(value.trim())) {
            fail(failureCode);
        }
    }

    private static String requireMatching(
            ConfigurableEnvironment environment,
            String key,
            String expectedValue,
            String failureCode) {
        String effectiveValue = required(environment, key);
        if (!expectedValue.equals(effectiveValue)) {
            fail(failureCode);
        }
        return effectiveValue;
    }

    private static void fail(String code) {
        throw new IllegalStateException(code);
    }
}
