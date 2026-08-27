package com.carebridge.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class RuntimeDatasourceEnvironmentPostProcessorTest {

    private final RuntimeDatasourceEnvironmentPostProcessor processor =
            new RuntimeDatasourceEnvironmentPostProcessor();

    @Test
    void completeCareBridgeTupleIsPublishedAtomically() {
        StandardEnvironment environment = environment(Map.of(
                "CAREBRIDGE_DB_URL", "jdbc:postgresql://primary.test:5432/carebridge",
                "CAREBRIDGE_DB_USERNAME", "primary-user",
                "CAREBRIDGE_DB_PASSWORD", "primary-password"));

        process(environment);

        assertDatasource(
                environment,
                "jdbc:postgresql://primary.test:5432/carebridge",
                "primary-user",
                "primary-password");
    }

    @Test
    void completeSupabaseTupleIsUsedOnlyWhenCareBridgeTupleIsAbsent() {
        StandardEnvironment environment = environment(Map.of(
                "SUPABASE_DB_URL", "jdbc:postgresql://fallback.test:6543/postgres",
                "SUPABASE_DB_USERNAME", "fallback-user",
                "SUPABASE_DB_PASSWORD", "fallback-password"));

        process(environment);

        assertDatasource(
                environment,
                "jdbc:postgresql://fallback.test:6543/postgres",
                "fallback-user",
                "fallback-password");
    }

    @Test
    void completeCareBridgeTupleWinsWhenBothTuplesAreComplete() {
        Map<String, Object> properties = completeCareBridgeTuple();
        properties.putAll(completeSupabaseTuple());
        StandardEnvironment environment = environment(properties);

        process(environment);

        assertDatasource(
                environment,
                "jdbc:postgresql://primary.test:5432/carebridge",
                "primary-user",
                "primary-password");
    }

    @Test
    void partialCareBridgeTupleRejectsEvenWithCompleteSupabaseTuple() {
        Map<String, Object> properties = completeSupabaseTuple();
        properties.put("CAREBRIDGE_DB_URL", "jdbc:postgresql://do-not-leak.test:5432/carebridge");
        assertRejected(
                properties,
                RuntimeDatasourceEnvironmentPostProcessor.CAREBRIDGE_CONFIGURATION_INCOMPLETE,
                "do-not-leak");
    }

    @Test
    void partialSupabaseTupleRejectsEvenWithCompleteCareBridgeTuple() {
        Map<String, Object> properties = completeCareBridgeTuple();
        properties.put("SUPABASE_DB_USERNAME", "do-not-leak-user");
        assertRejected(
                properties,
                RuntimeDatasourceEnvironmentPostProcessor.SUPABASE_CONFIGURATION_INCOMPLETE,
                "do-not-leak-user");
    }

    @Test
    void blankRuntimeValueCountsAsIncomplete() {
        Map<String, Object> properties = completeCareBridgeTuple();
        properties.put("CAREBRIDGE_DB_PASSWORD", " ");
        assertRejected(
                properties,
                RuntimeDatasourceEnvironmentPostProcessor.CAREBRIDGE_CONFIGURATION_INCOMPLETE,
                "primary-user");
    }

    @Test
    void higherPriorityPartialTupleCannotBorrowValuesFromLowerPrioritySource() {
        StandardEnvironment environment = environment(completeCareBridgeTuple());
        environment.getPropertySources().addFirst(new MapPropertySource(
                "higher-priority",
                Map.of("CAREBRIDGE_DB_URL", "jdbc:postgresql://ci.test:5432/carebridge")));

        assertThatThrownBy(() -> process(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RuntimeDatasourceEnvironmentPostProcessor.CAREBRIDGE_CONFIGURATION_INCOMPLETE)
                .hasMessageNotContaining("ci.test");
        assertThat(environment.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    void higherPriorityCompleteTupleOverridesLowerTupleAtomically() {
        StandardEnvironment environment = environment(completeCareBridgeTuple());
        environment.getPropertySources().addFirst(new MapPropertySource(
                "higher-priority",
                Map.of(
                        "CAREBRIDGE_DB_URL", "jdbc:postgresql://ci.test:5432/carebridge",
                        "CAREBRIDGE_DB_USERNAME", "ci-user",
                        "CAREBRIDGE_DB_PASSWORD", "ci-password")));

        process(environment);

        assertDatasource(
                environment,
                "jdbc:postgresql://ci.test:5432/carebridge",
                "ci-user",
                "ci-password");
    }

    @Test
    void completeDirectTupleWinsWhenEveryValueIsNonBlank() {
        StandardEnvironment environment = environment(Map.of(
                "spring.datasource.url", "jdbc:h2:mem:testdb",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "direct-password"));

        assertThatCode(() -> process(environment)).doesNotThrowAnyException();

        assertDatasource(environment, "jdbc:h2:mem:testdb", "sa", "direct-password");
        assertThat(environment.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    void blankDirectPasswordRejectsOutsideHermeticProfile() {
        for (String password : new String[] {"", " "}) {
            StandardEnvironment environment = environment(Map.of(
                    "spring.datasource.url", "jdbc:postgresql://runtime.test:5432/carebridge",
                    "spring.datasource.username", "runtime-user",
                    "spring.datasource.password", password));

            assertThatThrownBy(() -> process(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(RuntimeDatasourceEnvironmentPostProcessor.DIRECT_CONFIGURATION_INCOMPLETE);
        }
    }

    @Test
    void partialDirectTupleRejectsRatherThanBorrowingRuntimeCredentials() {
        Map<String, Object> properties = completeCareBridgeTuple();
        properties.put("spring.datasource.url", "jdbc:h2:mem:do-not-leak");
        assertRejected(
                properties,
                RuntimeDatasourceEnvironmentPostProcessor.DIRECT_CONFIGURATION_INCOMPLETE,
                "do-not-leak");
    }

    @Test
    void blankDirectUrlOrUsernameRejectsRatherThanBypassingTheGuard() {
        Map<String, Object> properties = completeCareBridgeTuple();
        properties.put("spring.datasource.url", " ");
        properties.put("spring.datasource.username", "");
        properties.put("spring.datasource.password", "allowed-empty-test-password");

        assertRejected(
                properties,
                RuntimeDatasourceEnvironmentPostProcessor.DIRECT_CONFIGURATION_INCOMPLETE,
                "allowed-empty-test-password");
    }

    @Test
    void missingRuntimeConfigurationFailsClosed() {
        assertRejected(
                Map.of(),
                RuntimeDatasourceEnvironmentPostProcessor.CONFIGURATION_MISSING,
                "password");
    }

    @Test
    void localProfileWithoutExplicitDatabaseOverrideLeavesProfileDefaultsAuthoritative() {
        StandardEnvironment local = environment(Map.of());
        local.setActiveProfiles("local");

        assertThatCode(() -> process(local)).doesNotThrowAnyException();
        assertThat(local.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    void localProfileRejectsPartialCareBridgeTuple() {
        StandardEnvironment local = environment(Map.of("CAREBRIDGE_DB_URL", "partial"));
        local.setActiveProfiles("local");

        assertThatThrownBy(() -> process(local))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RuntimeDatasourceEnvironmentPostProcessor.CAREBRIDGE_CONFIGURATION_INCOMPLETE);
    }

    @Test
    void localProfileRejectsPartialSupabaseTuple() {
        StandardEnvironment local = environment(Map.of("SUPABASE_DB_PASSWORD", "partial"));
        local.setActiveProfiles("local");

        assertThatThrownBy(() -> process(local))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RuntimeDatasourceEnvironmentPostProcessor.SUPABASE_CONFIGURATION_INCOMPLETE);
    }

    @Test
    void localProfilePublishesCompleteCareBridgeTupleAheadOfProfileDefaults() {
        StandardEnvironment local = environment(completeCareBridgeTuple());
        addLocalProfileDatasourceDefaults(local);
        local.setActiveProfiles("local");

        process(local);

        assertDatasource(
                local,
                "jdbc:postgresql://primary.test:5432/carebridge",
                "primary-user",
                "primary-password");
        assertThat(local.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isTrue();
    }

    @Test
    void localProfilePublishesCompleteSupabaseTupleWhenCareBridgeTupleIsAbsent() {
        StandardEnvironment local = environment(completeSupabaseTuple());
        addLocalProfileDatasourceDefaults(local);
        local.setActiveProfiles("local");

        process(local);

        assertDatasource(
                local,
                "jdbc:postgresql://fallback.test:6543/postgres",
                "fallback-user",
                "fallback-password");
        assertThat(local.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isTrue();
    }

    @Test
    void localProfilePreservesHigherPriorityCompleteDirectTuple() {
        StandardEnvironment local = environment(completeCareBridgeTuple());
        local.getPropertySources().addFirst(new MapPropertySource(
                "direct-override",
                Map.of(
                        "spring.datasource.url", "jdbc:postgresql://direct.test:5432/carebridge",
                        "spring.datasource.username", "direct-user",
                        "spring.datasource.password", "direct-password")));
        local.setActiveProfiles("local");

        process(local);

        assertDatasource(
                local,
                "jdbc:postgresql://direct.test:5432/carebridge",
                "direct-user",
                "direct-password");
        assertThat(local.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    void hermeticProfileRemainsDelegatedToHermeticProcessor() {
        StandardEnvironment hermetic = environment(Map.of("SUPABASE_DB_PASSWORD", "partial"));
        hermetic.setActiveProfiles("hermetic");

        assertThatCode(() -> process(hermetic)).doesNotThrowAnyException();
        assertThat(hermetic.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    void processorOrderingIsDotenvThenRuntimeThenHermetic() {
        assertThat(new DotenvEnvironmentPostProcessor().getOrder())
                .isEqualTo(Ordered.LOWEST_PRECEDENCE - 2);
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 1);
        assertThat(new HermeticDatasourceEnvironmentPostProcessor().getOrder())
                .isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private void assertRejected(Map<String, Object> properties, String code, String secret) {
        StandardEnvironment environment = environment(properties);

        assertThatThrownBy(() -> process(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(code)
                .hasMessageNotContaining(secret);
        assertThat(environment.getPropertySources()
                        .contains(RuntimeDatasourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    private static void assertDatasource(
            StandardEnvironment environment, String url, String username, String password) {
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo(url);
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo(username);
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo(password);
    }

    private static StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(
                StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("test", properties));
        }
        return environment;
    }

    private static Map<String, Object> completeCareBridgeTuple() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("CAREBRIDGE_DB_URL", "jdbc:postgresql://primary.test:5432/carebridge");
        properties.put("CAREBRIDGE_DB_USERNAME", "primary-user");
        properties.put("CAREBRIDGE_DB_PASSWORD", "primary-password");
        return properties;
    }

    private static Map<String, Object> completeSupabaseTuple() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("SUPABASE_DB_URL", "jdbc:postgresql://fallback.test:6543/postgres");
        properties.put("SUPABASE_DB_USERNAME", "fallback-user");
        properties.put("SUPABASE_DB_PASSWORD", "fallback-password");
        return properties;
    }

    private static void addLocalProfileDatasourceDefaults(StandardEnvironment environment) {
        environment.getPropertySources().addLast(new MapPropertySource(
                "local-profile-defaults",
                Map.of(
                        "spring.datasource.url", "jdbc:postgresql://localhost:5433/carebridge",
                        "spring.datasource.username", "carebridge",
                        "spring.datasource.password", "carebridge")));
    }

    private void process(StandardEnvironment environment) {
        processor.postProcessEnvironment(environment, new SpringApplication());
    }
}
