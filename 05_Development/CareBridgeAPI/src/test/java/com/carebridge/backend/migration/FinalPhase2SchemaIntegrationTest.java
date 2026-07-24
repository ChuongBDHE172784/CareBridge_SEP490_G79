package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class FinalPhase2SchemaIntegrationTest {

    private static final List<String> APPROVED_TARGET = List.of(
            "persons", "care_subjects", "users", "user_identities", "auth_sessions",
            "auth_revocations", "auth_challenges", "account_deletion_requests", "mother_journeys",
            "mother_journey_events", "maternal_observations", "maternal_exercise_sessions",
            "care_logs", "growth_measurements", "development_milestones", "vaccination_records",
            "vaccination_schedules", "community_profiles", "community_topics", "community_content",
            "community_interactions", "professional_profiles", "specialties",
            "professional_specialties", "expert_credentials", "expert_availability",
            "expert_location_shares", "expert_contribution_events", "triage_sessions",
            "triage_session_evidence", "red_flag_rules", "health_context_memories",
            "knowledge_sources", "knowledge_source_reviews", "health_records", "attachments",
            "health_record_attachments", "device_connections", "health_observations", "care_groups",
            "care_group_members", "scheduled_care_items", "family_tasks",
            "preparation_checklist_items", "care_item_templates", "content_items",
            "content_item_topics", "content_item_sources", "moderation_cases", "moderation_events",
            "notification_records", "device_tokens", "safety_configs", "safety_monitoring_sessions",
            "safety_events", "safety_event_actions", "emergency_contacts", "administrative_areas",
            "care_facilities", "nearby_support_requests", "nearby_support_responses", "audit_events",
            "security_events", "data_permissions", "system_configurations", "expense_entries",
            "archived_consultation_records", "archived_realtime_records", "archived_partner_records",
            "flyway_schema_history");

    private static final List<String> APPROVED_RELEASE1_EXTENSIONS = List.of(
            "expert_consultation_requests",
            "consultation_context_shares",
            "consultation_context_citations");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapContainsExactlyApprovedTargetTables() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        List<String> actual = tableNames();
        List<String> extra = actual.stream().filter(table -> !APPROVED_TARGET.contains(table)).toList();
        List<String> missing = APPROVED_TARGET.stream().filter(table -> !actual.contains(table)).toList();

        assertThat(missing).as("missing approved tables").isEmpty();
        assertThat(extra).as("unexpected non-canonical tables")
                .containsExactlyInAnyOrderElementsOf(APPROVED_RELEASE1_EXTENSIONS);
        assertThat(actual).hasSize(APPROVED_TARGET.size() + APPROVED_RELEASE1_EXTENSIONS.size());
    }

    private List<String> tableNames() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT table_name
                          FROM information_schema.tables
                         WHERE table_schema='public' AND table_type='BASE TABLE'
                         ORDER BY table_name
                        """)) {
            var names = new java.util.ArrayList<String>();
            while (result.next()) {
                names.add(result.getString(1));
            }
            return names;
        }
    }
}
