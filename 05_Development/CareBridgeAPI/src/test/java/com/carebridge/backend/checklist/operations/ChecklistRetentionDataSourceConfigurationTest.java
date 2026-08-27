package com.carebridge.backend.checklist.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class ChecklistRetentionDataSourceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("jdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withUserConfiguration(ChecklistRetentionDataSourceConfiguration.class);

    @Test
    void disabledConfigurationDoesNotAliasTheSharedApplicationDataSource() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("checklistRetentionDataSource");
            assertThat(context).doesNotHaveBean("checklistRetentionJdbcTemplate");
            assertThat(context).hasSingleBean(JdbcTemplate.class);
        });
    }

    @Test
    void enabledConfigurationFailsClosedWhenPasswordIsMissing() {
        contextRunner
                .withPropertyValues(
                        "carebridge.checklist.retention.datasource.enabled=true",
                        "carebridge.checklist.retention.datasource.url=jdbc:postgresql://db.test/carebridge",
                        "carebridge.checklist.retention.datasource.username=checklist_operations")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CHECKLIST_RETENTION_DATASOURCE_INCOMPLETE");
                });
    }

    @Test
    void enabledConfigurationRejectsTheSharedApplicationDatabaseRole() {
        contextRunner
                .withPropertyValues(
                        "carebridge.checklist.retention.datasource.enabled=true",
                        "carebridge.checklist.retention.datasource.url=jdbc:postgresql://db.test/carebridge",
                        "carebridge.checklist.retention.datasource.username=carebridge",
                        "carebridge.checklist.retention.datasource.password=operations-secret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CHECKLIST_RETENTION_DATASOURCE_ROLE_INVALID");
                });
    }

    @Test
    void enabledConfigurationCreatesDedicatedTemplateWithoutReplacingSharedTemplate() {
        contextRunner
                .withPropertyValues(
                        "carebridge.checklist.retention.datasource.enabled=true",
                        "carebridge.checklist.retention.datasource.url=jdbc:postgresql://db.test/carebridge",
                        "carebridge.checklist.retention.datasource.username=checklist_operations",
                        "carebridge.checklist.retention.datasource.password=operations-secret",
                        "carebridge.checklist.retention.datasource.runtime-username=carebridge_application",
                        "carebridge.checklist.retention.datasource.flyway-username=carebridge_flyway",
                        "carebridge.checklist.retention.datasource.application-flyway-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("checklistRetentionDataSource");
                    assertThat(context).hasBean("checklistRetentionJdbcTemplate");
                    assertThat(context).hasSingleBean(ChecklistRetentionOwnerIsolationVerifier.class);
                    assertThat(context.getBean("checklistRetentionJdbcTemplate"))
                            .isInstanceOf(JdbcTemplate.class);
                    assertThat(context.getBeansOfType(JdbcTemplate.class)).hasSize(2);
                });
    }

    @Test
    void enabledConfigurationRejectsRuntimeFlywayIdentityReuse() {
        contextRunner
                .withPropertyValues(
                        "carebridge.checklist.retention.datasource.enabled=true",
                        "carebridge.checklist.retention.datasource.url=jdbc:postgresql://db.test/carebridge",
                        "carebridge.checklist.retention.datasource.username=checklist_operations",
                        "carebridge.checklist.retention.datasource.password=operations-secret",
                        "carebridge.checklist.retention.datasource.runtime-username=carebridge_application",
                        "carebridge.checklist.retention.datasource.flyway-username=carebridge_application",
                        "carebridge.checklist.retention.datasource.application-flyway-enabled=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "CHECKLIST_RUNTIME_FLYWAY_IDENTITY_SEPARATION_REQUIRED");
                });
    }

    @Test
    void enabledConfigurationRejectsInProcessApplicationFlyway() {
        contextRunner
                .withPropertyValues(
                        "carebridge.checklist.retention.datasource.enabled=true",
                        "carebridge.checklist.retention.datasource.url=jdbc:postgresql://db.test/carebridge",
                        "carebridge.checklist.retention.datasource.username=checklist_operations",
                        "carebridge.checklist.retention.datasource.password=operations-secret",
                        "carebridge.checklist.retention.datasource.runtime-username=carebridge_application",
                        "carebridge.checklist.retention.datasource.flyway-username=carebridge_flyway",
                        "carebridge.checklist.retention.datasource.application-flyway-enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CHECKLIST_RUNTIME_FLYWAY_MUST_BE_DISABLED");
                });
    }

    @Test
    void applicationConfigHasNoDefaultOperationsSecret() throws Exception {
        String applicationYaml = Files.readString(
                Path.of("src", "main", "resources", "application.yaml"),
                StandardCharsets.UTF_8);

        assertThat(applicationYaml)
                .contains("CAREBRIDGE_CHECKLIST_RETENTION_DB_PASSWORD:")
                .doesNotContain("CAREBRIDGE_CHECKLIST_RETENTION_DB_PASSWORD:carebridge")
                .doesNotContain("CAREBRIDGE_CHECKLIST_RETENTION_DB_PASSWORD:checklist_operations");
    }
}
