package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import com.carebridge.backend.testsupport.MigrationLocator;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Mf14MigrationContractTest {

    // Resolved by script name: the migration keeps its description across re-versioning of the
    // Flyway chain, so pinning the version prefix here only produces false failures.
    private static final Path MIGRATION =
            MigrationLocator.byDescription("align_safety_action_persistence");

    @Test
    void migrationAlignsExistingCanonicalSafetyTableWithoutCreatingSchemaObjects() throws Exception {
        String sql = normalize(Files.readString(MIGRATION));

        assertThat(sql)
                .contains("alter table public.safety_events alter column alert_generation set default 0")
                .contains("alter column alert_successful_recipient_count set default 0")
                .contains("alter column alert_failed_recipient_count set default 0")
                .contains("'triage_escalation'")
                .doesNotContain("create table")
                .doesNotContain("add column");
    }

    @Test
    void migrationMakesAlertJournalIndexesGenerationAndPhaseAware() throws Exception {
        String sql = normalize(Files.readString(MIGRATION));

        assertThat(sql)
                .contains("drop index if exists public.safety_events_attempt_event_uk")
                .contains("create unique index safety_events_attempt_event_uk")
                .contains("parent_event_id, alert_generation, action_phase")
                .contains("where action_type = 'alert_attempt'")
                .contains("drop index if exists public.safety_events_delivery_token_uk")
                .contains("create unique index safety_events_delivery_token_uk")
                .contains("parent_event_id, alert_generation, device_token_id, action_phase")
                .contains("where action_type = 'delivery'")
                .contains("drop index if exists public.safety_events_family_alert_uk")
                .contains("create unique index safety_events_family_alert_uk")
                .contains("parent_event_id, alert_generation")
                .contains("where action_type = 'family_alert'");
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
