package com.carebridge.backend.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermeticProfileConfigurationTest {

    @Test
    void runtimeProfile_WithoutOverrides_RequiresExplicitDatasource() throws IOException {
        StandardEnvironment environment = loadConfiguration("application.yaml", Map.of());

        assertThatThrownBy(() -> processRuntimeDatasource(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RuntimeDatasourceEnvironmentPostProcessor.CONFIGURATION_MISSING);
    }

    @Test
    void runtimeProfile_WithLegacySupabaseDatabaseVariables_UsesExplicitFallback() throws IOException {
        StandardEnvironment environment = loadConfiguration("application.yaml", Map.of(
                "SUPABASE_DB_URL", "jdbc:postgresql://remote.example.test:6543/postgres",
                "SUPABASE_DB_USERNAME", "legacy-user",
                "SUPABASE_DB_PASSWORD", "legacy-password"));

        processRuntimeDatasource(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://remote.example.test:6543/postgres");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("legacy-user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("legacy-password");
    }

    @Test
    void runtimeProfile_WithGenericOverrides_UsesProvidedDatasource() throws IOException {
        StandardEnvironment environment = loadConfiguration("application.yaml", Map.of(
                "CAREBRIDGE_DB_URL", "jdbc:postgresql://127.0.0.1:5544/carebridge_ci",
                "CAREBRIDGE_DB_USERNAME", "ci-user",
                "CAREBRIDGE_DB_PASSWORD", "ci-password"));

        processRuntimeDatasource(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://127.0.0.1:5544/carebridge_ci");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("ci-user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("ci-password");
    }

    @Test
    void runtimeProfile_WithCompleteDirectOverrides_PreservesTheCanonicalTuple()
            throws IOException {
        StandardEnvironment environment = loadConfiguration("application.yaml", Map.of(
                "spring.datasource.url", "jdbc:h2:mem:direct-test",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "direct-password"));

        processRuntimeDatasource(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:h2:mem:direct-test");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("sa");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("direct-password");
    }

    @Test
    void localProfile_WithoutOverrides_UsesLoopbackDockerDatasourceDefaults() throws IOException {
        StandardEnvironment environment = loadConfiguration("application-local.yaml", Map.of());
        environment.setActiveProfiles("local");

        assertThatCode(() -> processRuntimeDatasource(environment)).doesNotThrowAnyException();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5433/carebridge");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("carebridge");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("carebridge");
    }

    @Test
    void hermeticProfile_WithoutOverrides_FailsClosed() throws IOException {
        StandardEnvironment environment = loadConfiguration("application-hermetic.yaml", Map.of());
        environment.setActiveProfiles("hermetic");

        assertThatCode(() -> processRuntimeDatasource(environment)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new HermeticDatasourceEnvironmentPostProcessor()
                        .postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HERMETIC_DATASOURCE_MISSING");
    }

    @Test
    void hermeticProfile_WithEnvironmentOverrides_UsesProvidedLocalTestDatasource()
            throws IOException {
        StandardEnvironment environment = loadConfiguration("application-hermetic.yaml", Map.of(
                "CAREBRIDGE_HERMETIC_DB_URL", "jdbc:postgresql://127.0.0.1:5544/carebridge_ci_test",
                "CAREBRIDGE_HERMETIC_DB_USERNAME", "ci-test-user",
                "CAREBRIDGE_HERMETIC_DB_PASSWORD", "ci-test-password",
                "CAREBRIDGE_HERMETIC_DB_DATABASE_NAME", "carebridge_ci_test",
                "CAREBRIDGE_HERMETIC_DB_SCHEMA", "ci_test"));
        environment.setActiveProfiles("hermetic");

        assertThatCode(() -> processRuntimeDatasource(environment)).doesNotThrowAnyException();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://127.0.0.1:5544/carebridge_ci_test");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("ci-test-user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("ci-test-password");
        assertThat(environment.getProperty("carebridge.hermetic.datasource.database-name"))
                .isEqualTo("carebridge_ci_test");
        assertThat(environment.getProperty("spring.datasource.hikari.schema")).isEqualTo("ci_test");
        assertThatCode(() -> new HermeticDatasourceEnvironmentPostProcessor()
                        .postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void hermeticProfile_WithDedicatedOverrides_UsesDynamicDatasourceAndSafePersistenceSettings()
            throws IOException {
        StandardEnvironment environment = loadConfiguration("application-hermetic.yaml", Map.of(
                "carebridge.hermetic.datasource.url", "jdbc:postgresql://127.0.0.1:5432/carebridge_test",
                "carebridge.hermetic.datasource.username", "test-user",
                "carebridge.hermetic.datasource.password", "test-password",
                "carebridge.hermetic.datasource.database-name", "carebridge_test",
                "carebridge.hermetic.datasource.schema", "batch4_test"));
        environment.setActiveProfiles("hermetic");

        assertThatCode(() -> processRuntimeDatasource(environment)).doesNotThrowAnyException();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://127.0.0.1:5432/carebridge_test");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("test-user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("test-password");
        assertThat(environment.getProperty("spring.datasource.hikari.schema"))
                .isEqualTo("batch4_test");
        assertThat(environment.getProperty("carebridge.dotenv.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("carebridge.datasource-guard.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.dialect"))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.flyway.default-schema")).isEqualTo("batch4_test");
        assertThat(environment.getProperty("spring.flyway.schemas")).isEqualTo("batch4_test");
        assertThat(environment.getProperty("carebridge.dev-seed.enabled", Boolean.class)).isFalse();
    }

    @Test
    void springFactories_RegistersAllEnvironmentPostProcessors() throws IOException {
        String factories = new ClassPathResource("META-INF/spring.factories")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(factories)
                .contains(DotenvEnvironmentPostProcessor.class.getName())
                .contains(RuntimeDatasourceEnvironmentPostProcessor.class.getName())
                .contains(HermeticDatasourceEnvironmentPostProcessor.class.getName());
    }

    @Test
    void productionFlywayDefaults_AreStrictForTheSquashedBaseline() throws IOException {
        StandardEnvironment environment = loadConfiguration("application.yaml", Map.of());

        assertThat(environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.flyway.out-of-order", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("spring.flyway.ignore-migration-patterns"))
                .isEqualTo("*:future");
    }

    private void processRuntimeDatasource(StandardEnvironment environment) {
        new RuntimeDatasourceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());
    }

    private StandardEnvironment loadConfiguration(String resource, Map<String, Object> overrides)
            throws IOException {
        Resource configurationResource = new FileSystemResource("src/main/resources/" + resource);
        List<PropertySource<?>> loaded = new YamlPropertySourceLoader().load(
                resource, configurationResource);
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if (!overrides.isEmpty()) {
            propertySources.addFirst(new MapPropertySource("overrides", overrides));
        }
        loaded.forEach(propertySources::addLast);
        return environment;
    }
}
