package com.carebridge.backend.checklist.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carebridge.checklist.retention.datasource")
public record ChecklistRetentionDataSourceProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String runtimeUsername,
        String flywayUsername,
        boolean applicationFlywayEnabled) {
}
