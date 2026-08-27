package com.carebridge.backend.checklist.operations;

import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistRetentionPurgeRepository {

    private final ObjectProvider<JdbcTemplate> dedicatedJdbcTemplateProvider;

    public ChecklistRetentionPurgeRepository(
            @Qualifier("checklistRetentionJdbcTemplate")
            ObjectProvider<JdbcTemplate> dedicatedJdbcTemplateProvider) {
        this.dedicatedJdbcTemplateProvider = dedicatedJdbcTemplateProvider;
    }

    public ChecklistRetentionPurgeResult purge(UUID actorUserId) {
        JdbcTemplate jdbcTemplate = dedicatedJdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("CHECKLIST_RETENTION_DATASOURCE_NOT_CONFIGURED");
        }
        return jdbcTemplate.queryForObject(
                """
                SELECT audit_events_purged, action_commands_purged
                FROM public.checklist_purge_retained_records(?)
                """,
                (resultSet, rowNumber) -> new ChecklistRetentionPurgeResult(
                        resultSet.getLong("audit_events_purged"),
                        resultSet.getLong("action_commands_purged")),
                actorUserId);
    }
}
