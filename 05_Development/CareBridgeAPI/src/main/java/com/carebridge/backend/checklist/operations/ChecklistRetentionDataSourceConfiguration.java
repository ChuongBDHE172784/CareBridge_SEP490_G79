package com.carebridge.backend.checklist.operations;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChecklistRetentionDataSourceProperties.class)
public class ChecklistRetentionDataSourceConfiguration {

    private static final String OPERATIONS_ROLE = "checklist_operations";
    private static final String APPLICATION_ROLE = "carebridge_application";

    @Bean("checklistRetentionJdbcTemplate")
    @ConditionalOnProperty(
            prefix = "carebridge.checklist.retention.datasource",
            name = "enabled",
            havingValue = "true")
    JdbcTemplate checklistRetentionJdbcTemplate(ChecklistRetentionDataSourceProperties properties) {
        validate(properties);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        return new JdbcTemplate((DataSource) dataSource);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "carebridge.checklist.retention.datasource",
            name = "enabled",
            havingValue = "true")
    ChecklistRetentionOwnerIsolationVerifier checklistRetentionOwnerIsolationVerifier(
            @Qualifier("checklistRetentionJdbcTemplate") JdbcTemplate dedicatedJdbcTemplate) {
        return new ChecklistRetentionOwnerIsolationVerifier(dedicatedJdbcTemplate);
    }

    private static void validate(ChecklistRetentionDataSourceProperties properties) {
        if (!StringUtils.hasText(properties.url())
                || !StringUtils.hasText(properties.username())
                || !StringUtils.hasText(properties.password())) {
            throw new IllegalStateException("CHECKLIST_RETENTION_DATASOURCE_INCOMPLETE");
        }
        if (!OPERATIONS_ROLE.equals(properties.username())) {
            throw new IllegalStateException("CHECKLIST_RETENTION_DATASOURCE_ROLE_INVALID");
        }
        if (!APPLICATION_ROLE.equals(properties.runtimeUsername())
                || !StringUtils.hasText(properties.flywayUsername())
                || APPLICATION_ROLE.equals(properties.flywayUsername())
                || OPERATIONS_ROLE.equals(properties.flywayUsername())
                || "carebridge_checklist_retention_owner".equals(properties.flywayUsername())
                || "carebridge_checklist_schema_owner".equals(properties.flywayUsername())) {
            throw new IllegalStateException("CHECKLIST_RUNTIME_FLYWAY_IDENTITY_SEPARATION_REQUIRED");
        }
        if (properties.applicationFlywayEnabled()) {
            throw new IllegalStateException("CHECKLIST_RUNTIME_FLYWAY_MUST_BE_DISABLED");
        }
    }
}
