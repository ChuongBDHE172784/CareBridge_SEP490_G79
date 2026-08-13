package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Live-upgrade evidence for homogeneous POSTPARTUM catalog routing. */
@EnabledOnOs(OS.WINDOWS)
class PostpartumChecklistCatalogSplitEmbeddedPostgresTest {

    private static final String BEFORE = "20260813140000";
    private static final String REPAIR = "20260813160000";
    private static final UUID KNOWN_BABY_ROOT =
            UUID.fromString("784bb631-1272-4f7c-9898-a01d8f1be93a");

    @Test
    @Timeout(180)
    void splitsFutureCatalogAndPreservesPinnedHistory() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30)).setServerConfig("max_connections", "100").start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            assertThat(flyway(dataSource, BEFORE).migrate().success).isTrue();

            Fixture fixture = seed(dataSource);
            var upgrade = flyway(dataSource, REPAIR);
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(2);
            assertThat(upgrade.migrate().migrationsExecuted).isZero();
            assertThat(upgrade.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates
                         where entry_type = 'TEMPLATE_ROOT' and stage = 'POSTPARTUM'
                           and content_status = 'APPROVED' and distribution_enabled
                           and eligibility_anchor_type = 'DELIVERY_DATE'
                        """)).isEqualTo(1);
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates
                         where entry_type = 'TEMPLATE_ROOT' and stage = 'BABY_CARE'
                           and content_status = 'APPROVED' and distribution_enabled
                           and eligibility_anchor_type = 'BIRTH_DATE'
                        """)).isEqualTo(1);
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates root
                        join public.care_item_templates item on item.parent_template_id = root.template_id
                         where root.stage = 'POSTPARTUM' and root.distribution_enabled
                           and ((root.eligibility_anchor_type = 'DELIVERY_DATE' and item.target_subject <> 'MOTHER')
                             or (root.eligibility_anchor_type = 'BIRTH_DATE' and item.target_subject <> 'BABY'))
                        """)).isZero();
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates root
                        join public.care_item_templates item on item.parent_template_id = root.template_id
                         where root.stage = 'BABY_CARE' and root.distribution_enabled
                           and ((coalesce(item.checklist_contract_version, 1) = 1
                                 and item.target_subject <> 'BABY')
                             or (item.checklist_contract_version = 2
                                 and (item.target_subject is not null
                                      or item.due_anchor_type <> 'BIRTH_DATE')))
                        """)).isZero();
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates
                         where template_id = '784bb631-1272-4f7c-9898-a01d8f1be93a'::uuid
                           and content_status = 'ARCHIVED' and not distribution_enabled
                        """)).isOne();
                assertThat(count(connection, """
                        select count(*) from public.care_item_templates
                         where template_id = public.checklist_p2_deterministic_uuid(
                           'POSTPARTUM_BABY_STAGE_REPAIR|ROOT|784bb631-1272-4f7c-9898-a01d8f1be93a')
                           and stage = 'BABY_CARE' and content_status = 'DRAFT'
                           and not distribution_enabled and migration_review_required
                        """)).isOne();
                try (var sql = connection.prepareStatement("""
                        select template_version_id, template_item_version_id, title_snapshot
                          from public.checklist_task_instances where checklist_task_instance_id = ?
                        """)) {
                    sql.setObject(1, fixture.taskId());
                    try (var row = sql.executeQuery()) {
                        assertThat(row.next()).isTrue();
                        assertThat(row.getObject(1, UUID.class)).isEqualTo(fixture.sourceVersion());
                        assertThat(row.getObject(2, UUID.class)).isEqualTo(fixture.motherItem());
                        assertThat(row.getString(3)).isEqualTo("Historical mother snapshot");
                    }
                }
            }
        }
    }

    private static Fixture seed(DataSource dataSource) throws Exception {
        UUID root = UUID.randomUUID(), version = UUID.randomUUID();
        UUID motherItem = UUID.randomUUID(), babyItem = UUID.randomUUID();
        UUID owner = UUID.randomUUID(), journey = UUID.randomUUID();
        UUID instance = UUID.randomUUID(), task = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            // Fixture-only setup bypasses unrelated ownership/context FKs and
            // approved-version mutation guards. Flyway itself runs normally.
            statement.execute("set local session_replication_role = replica");
            statement.execute("set constraints all deferred");
            try (var sql = connection.prepareStatement("""
                    insert into public.care_item_templates (
                      template_id, entry_type, title, stage, is_active, version,
                      template_status, content_status, template_lineage_id, template_version_id,
                      migration_review_required, distribution_enabled, approved_at, approved_by,
                      template_type, recipient_scope, eligibility_anchor_type, eligibility_range_unit,
                      eligibility_start_inclusive, eligibility_end_inclusive, schedule_type,
                      materialization_policy, schedule_context_type, checklist_contract_version,
                      created_at, updated_at)
                    values (?, 'TEMPLATE_ROOT', 'Mixed postpartum', 'POSTPARTUM', true, 1,
                      'ACTIVE', 'APPROVED', ?, ?, false, true, now(), ?, 'MANDATORY', 'MOTHER',
                      'DELIVERY_DATE', 'DAY', 0, 42, 'LEGACY', 'LEGACY_WINDOW', 'JOURNEY', 1, now(), now())
                    """)) {
                sql.setObject(1, root); sql.setObject(2, root); sql.setObject(3, version); sql.setObject(4, owner);
                assertThat(sql.executeUpdate()).isOne();
            }
            insertItem(connection, motherItem, root, "MOTHER", "DELIVERY_DATE", 1);
            insertItem(connection, babyItem, root, "BABY", "BIRTH_DATE", 2);
            try (var sql = connection.prepareStatement("""
                    insert into public.care_item_templates (
                      template_id, entry_type, title, stage, is_active, version,
                      template_status, content_status, template_lineage_id, template_version_id,
                      migration_review_required, distribution_enabled, approved_at, approved_by,
                      template_type, recipient_scope, eligibility_anchor_type, eligibility_range_unit,
                      eligibility_start_inclusive, eligibility_end_inclusive, schedule_type,
                      materialization_policy, schedule_context_type, checklist_contract_version,
                      created_at, updated_at)
                    values (?, 'TEMPLATE_ROOT', 'Known deployed baby checklist', 'POSTPARTUM', true, 1,
                      'ACTIVE', 'APPROVED', ?, ?, false, true, now(), ?, 'MANDATORY', 'MOTHER',
                      'DELIVERY_DATE', 'DAY', 0, 42, 'LEGACY', 'LEGACY_WINDOW', 'JOURNEY', 1, now(), now())
                    """)) {
                sql.setObject(1, KNOWN_BABY_ROOT); sql.setObject(2, KNOWN_BABY_ROOT);
                sql.setObject(3, KNOWN_BABY_ROOT); sql.setObject(4, owner);
                assertThat(sql.executeUpdate()).isOne();
            }
            insertItem(connection, UUID.randomUUID(), KNOWN_BABY_ROOT,
                    "MOTHER", "DELIVERY_DATE", 1);
            try (var sql = connection.prepareStatement("""
                    insert into public.checklist_instances (
                      checklist_instance_id, distribution_key, key_version, template_lineage_id,
                      template_version_id, recipient_user_id, recipient_role, care_context_type,
                      care_context_id, context_owner_user_id, origin, status, lock_version,
                      completed_at, historical_at, history_reason_code, checklist_contract_version,
                      created_at, updated_at)
                    values (?, ?, 'v1', ?, ?, ?, 'MOTHER', 'JOURNEY', ?, ?, 'SYSTEM_TEMPLATE',
                      'COMPLETED', 3, now(), now(), 'SEQUENCE_STEP_COMPLETED', 1, now(), now())
                    """)) {
                sql.setObject(1, instance); sql.setString(2, key(instance)); sql.setObject(3, root);
                sql.setObject(4, version); sql.setObject(5, owner); sql.setObject(6, journey); sql.setObject(7, owner);
                assertThat(sql.executeUpdate()).isOne();
            }
            try (var sql = connection.prepareStatement("""
                    insert into public.checklist_task_instances (
                      checklist_task_instance_id, checklist_instance_id, template_version_id,
                      template_item_version_id, task_key, key_version, title_snapshot, display_order,
                      is_required, target_subject, checklist_contract_version, status, lock_version,
                      completed_at, created_at, updated_at)
                    values (?, ?, ?, ?, ?, 'v1', 'Historical mother snapshot', 1, true, 'MOTHER',
                      1, 'COMPLETED', 4, now(), now(), now())
                    """)) {
                sql.setObject(1, task); sql.setObject(2, instance); sql.setObject(3, version);
                sql.setObject(4, motherItem); sql.setString(5, key(task));
                assertThat(sql.executeUpdate()).isOne();
            }
            connection.commit();
        }
        return new Fixture(version, motherItem, task);
    }

    private static void insertItem(Connection connection, UUID id, UUID root, String target,
                                   String anchor, int order) throws Exception {
        try (var sql = connection.prepareStatement("""
                insert into public.care_item_templates (
                  template_id, parent_template_id, entry_type, title, stage, is_active, version,
                  template_status, content_status, display_order, is_required, target_subject,
                  due_anchor_type, due_offset_start, due_offset_end, due_offset_unit,
                  checklist_contract_version, created_at, updated_at)
                values (?, ?, 'CHECKLIST_ENTRY', ?, 'POSTPARTUM', true, 1, 'ACTIVE', 'APPROVED',
                  ?, true, ?, ?, 0, 42, 'DAY', 1, now(), now())
                """)) {
            sql.setObject(1, id); sql.setObject(2, root); sql.setString(3, target + " item");
            sql.setInt(4, order); sql.setString(5, target); sql.setString(6, anchor);
            assertThat(sql.executeUpdate()).isOne();
        }
    }

    private static long count(Connection connection, String query) throws Exception {
        try (var statement = connection.createStatement(); var row = statement.executeQuery(query)) {
            assertThat(row.next()).isTrue(); return row.getLong(1);
        }
    }

    private static String key(UUID id) { return id.toString().replace("-", "") + "0".repeat(32); }

    private static Flyway flyway(DataSource source, String target) {
        return Flyway.configure().dataSource(source).locations("classpath:db/migration")
                .target(target).cleanDisabled(true).validateOnMigrate(true)
                .ignoreMigrationPatterns("*:future").load();
    }

    private record Fixture(UUID sourceVersion, UUID motherItem, UUID taskId) {}
}
