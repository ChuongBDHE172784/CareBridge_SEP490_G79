package com.carebridge.backend.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class HermeticProfileConfigurationTest {

    @Test
    void hermeticProfile_UsesMandatoryDedicatedDatasourceAndSafePersistenceSettings() throws IOException {
        List<PropertySource<?>> loaded = new YamlPropertySourceLoader().load(
                "hermetic", new ClassPathResource("application-hermetic.yaml"));
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new org.springframework.core.env.MapPropertySource("requiredHermeticValues", Map.of(
                "carebridge.hermetic.datasource.url", "jdbc:postgresql://127.0.0.1:5432/carebridge_test",
                "carebridge.hermetic.datasource.username", "test-user",
                "carebridge.hermetic.datasource.password", "test-password",
                "carebridge.hermetic.datasource.database-name", "carebridge_test",
                "carebridge.hermetic.datasource.schema", "batch4_test")));
        loaded.forEach(propertySources::addLast);

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
    void springFactories_RegistersBothEnvironmentPostProcessors() throws IOException {
        String factories = new ClassPathResource("META-INF/spring.factories")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(factories)
                .contains(DotenvEnvironmentPostProcessor.class.getName())
                .contains(HermeticDatasourceEnvironmentPostProcessor.class.getName());
    }
}
