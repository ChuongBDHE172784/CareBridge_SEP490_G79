package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Runs the complete migration chain on a real Docker-free PostgreSQL 18 process. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistFlywayEmbeddedPostgresTest {

    @Test
    @Timeout(180)
    void fullFlywayChainIsRepeatableAndChecklistSchemaIsOperational() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();

            var first = flyway.migrate();
            var second = flyway.migrate();

            assertThat(first.success).isTrue();
            assertThat(second.success).isTrue();
            assertThat(second.migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                 var statement = connection.createStatement()) {
                try (var currentVersion = statement.executeQuery("""
                        select version
                          from public.flyway_schema_history
                         where success
                         order by installed_rank desc
                         limit 1
                        """)) {
                    assertThat(currentVersion.next()).isTrue();
                        assertThat(currentVersion.getString(1)).isEqualTo("20260813140000");
                }
                try (var version = statement.executeQuery("show server_version")) {
                    assertThat(version.next()).isTrue();
                    assertThat(version.getString(1)).startsWith("18.1");
                }
                try (var schema = statement.executeQuery("""
                        select
                          to_regclass('public.checklist_instances') is not null,
                          to_regclass('public.checklist_task_instances') is not null,
                          to_regclass('public.checklist_action_commands') is not null,
                          to_regclass('public.checklist_reconciliation_candidates') is null,
                          to_regclass('public.checklist_reconciliation_runs') is null,
                          to_regclass('public.checklist_distribution_outbox') is null,
                          to_regclass('public.checklist_migration_quarantine') is null,
                          to_regclass('public.checklist_care_group_contexts') is null,
                          to_regclass('public.checklist_context_authorities') is null,
                          to_regclass('public.checklist_template_version_items') is null,
                          to_regclass('public.checklist_template_recipient_roles') is null,
                          to_regclass('public.checklist_substages') is null
                        """)) {
                    assertThat(schema.next()).isTrue();
                    for (int column = 1; column <= 12; column++) {
                        assertThat(schema.getBoolean(column))
                                .as("retirement catalog assertion %s", column)
                                .isTrue();
                    }
                }
                try (var pregnancy = statement.executeQuery("""
                        select count(*) filter (where entry_type = 'TEMPLATE_ROOT'
                                                 and stage = 'PREGNANCY'
                                                 and checklist_contract_version = 2) as roots,
                               count(*) filter (where entry_type = 'TEMPLATE_ROOT'
                                                  and stage = 'PREGNANCY'
                                                  and checklist_contract_version = 2
                                                  and target_subject is null
                                                  and is_required is null) as targetless_roots,
                               count(*) filter (where entry_type = 'CHECKLIST_ENTRY'
                                                  and stage = 'PREGNANCY'
                                                  and checklist_contract_version = 2) as leaves,
                               count(*) filter (where entry_type = 'CHECKLIST_ENTRY'
                                                  and stage = 'PREGNANCY'
                                                  and checklist_contract_version = 2
                                                  and target_subject is null
                                                  and is_required is not null) as required_leaves,
                               count(*) filter (where entry_type = 'TEMPLATE_ROOT'
                                                  and stage = 'PREGNANCY'
                                                  and checklist_contract_version = 2
                                                  and schedule_type = 'DAILY') as daily_roots,
                               count(*) filter (where entry_type = 'TEMPLATE_ROOT'
                                                 and stage = 'PREGNANCY'
                                                 and checklist_contract_version = 2
                                                 and content_status = 'DRAFT'
                                                 and distribution_enabled = false
                                                 and checklist_metadata_jsonb ->> 'provenanceStatus'
                                                     = 'PENDING_CLINICAL_COPY_SIGN_OFF') as pending,
                               min(eligibility_start_inclusive) filter (
                                   where entry_type = 'TEMPLATE_ROOT'
                                     and stage = 'PREGNANCY'
                                     and checklist_contract_version = 2
                                     and checklist_metadata_jsonb ->> 'plan' = '2') as plan_two_start
                          from public.care_item_templates
                        """)) {
                    assertThat(pregnancy.next()).isTrue();
                    assertThat(pregnancy.getInt("roots")).isEqualTo(16);
                    assertThat(pregnancy.getInt("targetless_roots")).isEqualTo(16);
                    assertThat(pregnancy.getInt("leaves")).isEqualTo(62);
                    assertThat(pregnancy.getInt("required_leaves")).isEqualTo(62);
                    assertThat(pregnancy.getInt("daily_roots")).isZero();
                    assertThat(pregnancy.getInt("pending")).isEqualTo(16);
                    assertThat(pregnancy.getInt("plan_two_start")).isEqualTo(20);
                }
                String rootId;
                try (var root = statement.executeQuery("""
                        select template_id
                          from public.care_item_templates
                         where entry_type = 'TEMPLATE_ROOT'
                           and stage = 'PREGNANCY'
                           and checklist_contract_version = 2
                         order by template_id
                         limit 1
                        """)) {
                    assertThat(root.next()).isTrue();
                    rootId = root.getString(1);
                }
                String rootPredicate = " where template_id = '" + rootId + "'::uuid";
                statement.executeUpdate(
                        "update public.care_item_templates "
                                + "set content_status = 'PENDING_REVIEW', "
                                + "migration_review_required = false, "
                                + "migration_reviewed_at = now(), "
                                + "migration_reviewed_by = '10000000-0000-0000-0000-000000000001'::uuid, "
                                + "distribution_enabled = false, approved_at = null, approved_by = null"
                                + rootPredicate);
                statement.executeUpdate(
                        "update public.care_item_templates "
                                + "set content_status = 'APPROVED', distribution_enabled = true, "
                                + "approved_at = now(), "
                                + "approved_by = '10000000-0000-0000-0000-000000000001'::uuid"
                                + rootPredicate);
                try (var activated = statement.executeQuery(
                        "select content_status, distribution_enabled "
                                + "from public.care_item_templates" + rootPredicate)) {
                    assertThat(activated.next()).isTrue();
                    assertThat(activated.getString("content_status")).isEqualTo("APPROVED");
                    assertThat(activated.getBoolean("distribution_enabled")).isTrue();
                }
            }
        }
    }
}
