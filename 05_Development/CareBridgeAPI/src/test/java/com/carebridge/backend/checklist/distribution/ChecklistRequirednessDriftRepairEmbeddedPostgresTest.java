package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Live-upgrade evidence for the scoped PRE_PREGNANCY requiredness repair. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistRequirednessDriftRepairEmbeddedPostgresTest {

    private static final String PRE_REPAIR_VERSION = "20260813130000";
    private static final String REPAIR_VERSION = "20260813140000";
    private static final Instant COMPLETED_AT = Instant.parse("2026-08-12T03:04:05Z");
    private static final Instant ORIGINAL_UPDATED_AT = Instant.parse("2026-08-12T04:05:06Z");

    @Test
    @Timeout(180)
    void repairsOnlyCurrentSystemSequenceSnapshotAndSecondMigrateIsNoOp() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);

            Flyway beforeRepair = flyway(dataSource, PRE_REPAIR_VERSION);
            assertThat(beforeRepair.migrate().success).isTrue();

            Fixture fixture = seedDriftFixture(dataSource);
            Map<UUID, TaskSnapshot> before = snapshots(dataSource, fixture);
            assertThat(before.values()).allMatch(snapshot -> !snapshot.required());

            Flyway upgraded = flyway(dataSource, REPAIR_VERSION);
            var firstUpgrade = upgraded.migrate();
            var secondUpgrade = upgraded.migrate();

            assertThat(firstUpgrade.success).isTrue();
            assertThat(firstUpgrade.migrationsExecuted).isOne();
            assertThat(secondUpgrade.success).isTrue();
            assertThat(secondUpgrade.migrationsExecuted).isZero();
            assertThat(upgraded.validateWithResult().validationSuccessful).isTrue();

            Map<UUID, TaskSnapshot> after = snapshots(dataSource, fixture);
            assertThat(after.get(fixture.currentTask()).required()).isTrue();
            assertLifecyclePreserved(before.get(fixture.currentTask()), after.get(fixture.currentTask()));

            assertThat(after.get(fixture.historicalTask()))
                    .isEqualTo(before.get(fixture.historicalTask()));
            assertThat(after.get(fixture.userCreatedTask()))
                    .isEqualTo(before.get(fixture.userCreatedTask()));
            assertThat(after.get(fixture.nonSequenceTask()))
                    .isEqualTo(before.get(fixture.nonSequenceTask()));
        }
    }

    private static Fixture seedDriftFixture(DataSource dataSource) throws Exception {
        UUID sequenceRoot = UUID.randomUUID();
        UUID sequenceVersion = UUID.randomUUID();
        UUID sequenceItem = UUID.randomUUID();
        UUID nonSequenceRoot = UUID.randomUUID();
        UUID nonSequenceVersion = UUID.randomUUID();
        UUID nonSequenceItem = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID journey = UUID.randomUUID();

        UUID currentParent = UUID.randomUUID();
        UUID historicalParent = UUID.randomUUID();
        UUID userCreatedParent = UUID.randomUUID();
        UUID nonSequenceParent = UUID.randomUUID();
        Fixture fixture = new Fixture(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("set local session_replication_role = replica");

            insertRoot(connection, sequenceRoot, sequenceVersion, 1);
            insertLeaf(connection, sequenceItem, sequenceRoot, true);
            insertRoot(connection, nonSequenceRoot, nonSequenceVersion, 0);
            insertLeaf(connection, nonSequenceItem, nonSequenceRoot, true);
            insertParent(connection, currentParent, sequenceRoot, sequenceVersion, owner, journey,
                    "SYSTEM_TEMPLATE", null, null);
            insertParent(connection, historicalParent, sequenceRoot, sequenceVersion, owner, journey,
                    "SYSTEM_TEMPLATE", Instant.parse("2026-08-13T00:00:00Z"),
                    "SEQUENCE_STEP_COMPLETED");
            insertParent(connection, userCreatedParent, null, null, owner, journey,
                    "USER_CREATED", null, null);
            insertParent(connection, nonSequenceParent, nonSequenceRoot, nonSequenceVersion,
                    owner, journey, "SYSTEM_TEMPLATE", null, null);

            insertTask(connection, fixture.currentTask(), currentParent,
                    sequenceVersion, sequenceItem, 1);
            insertTask(connection, fixture.historicalTask(), historicalParent,
                    sequenceVersion, sequenceItem, 2);
            insertTask(connection, fixture.userCreatedTask(), userCreatedParent,
                    null, null, 3);
            insertTask(connection, fixture.nonSequenceTask(), nonSequenceParent,
                    nonSequenceVersion, nonSequenceItem, 4);
            connection.commit();
        }
        return fixture;
    }

    private static void insertRoot(Connection connection, UUID rootId, UUID versionId,
                                   int sequencePosition) throws Exception {
        try (var sql = connection.prepareStatement("""
                insert into public.care_item_templates (
                    template_id, entry_type, title, stage, is_active, version,
                    template_status, content_status, template_lineage_id,
                    template_version_id, migration_review_required, distribution_enabled,
                    template_type, recipient_scope, eligibility_anchor_type,
                    eligibility_range_unit, eligibility_start_inclusive,
                    eligibility_end_inclusive, display_order, checklist_contract_version,
                    created_at, updated_at)
                values (?, 'TEMPLATE_ROOT', ?, 'PRE_PREGNANCY', true, 1,
                    'ACTIVE', 'DRAFT', ?, ?, false, false, 'MANDATORY', 'MOTHER',
                    'NONE', 'DAY', 0, 0, ?, 2, now(), now())
                """)) {
            sql.setObject(1, rootId);
            sql.setString(2, "Requiredness repair root " + sequencePosition);
            sql.setObject(3, rootId);
            sql.setObject(4, versionId);
            sql.setInt(5, sequencePosition);
            assertThat(sql.executeUpdate()).isOne();
        }
    }

    private static void insertLeaf(Connection connection, UUID itemId, UUID rootId,
                                   boolean required) throws Exception {
        try (var sql = connection.prepareStatement("""
                insert into public.care_item_templates (
                    template_id, parent_template_id, entry_type, title, stage, is_active,
                    version, template_status, content_status, display_order, is_required,
                    checklist_contract_version, created_at, updated_at)
                values (?, ?, 'CHECKLIST_ENTRY', 'Required leaf', 'PRE_PREGNANCY', true,
                    1, 'ACTIVE', 'APPROVED', 1, ?, 2, now(), now())
                """)) {
            sql.setObject(1, itemId);
            sql.setObject(2, rootId);
            sql.setBoolean(3, required);
            assertThat(sql.executeUpdate()).isOne();
        }
    }

    private static void insertParent(Connection connection, UUID parentId, UUID lineageId,
                                     UUID versionId, UUID owner, UUID journey, String origin,
                                     Instant historicalAt, String historyReason) throws Exception {
        try (var sql = connection.prepareStatement("""
                insert into public.checklist_instances (
                    checklist_instance_id, distribution_key, key_version,
                    template_lineage_id, template_version_id, recipient_user_id,
                    recipient_role, care_group_id, care_context_type, care_context_id,
                    context_owner_user_id, origin, status, lock_version, completed_at,
                    historical_at, history_reason_code, created_at, updated_at)
                values (?, ?, 'v1', ?, ?, ?, 'MOTHER', null, 'JOURNEY', ?, ?, ?,
                    'COMPLETED', 7, ?, ?, ?, now(), ?)
                """)) {
            sql.setObject(1, parentId);
            sql.setString(2, hexKey(parentId));
            sql.setObject(3, lineageId);
            sql.setObject(4, versionId);
            sql.setObject(5, owner);
            sql.setObject(6, journey);
            sql.setObject(7, owner);
            sql.setString(8, origin);
            sql.setTimestamp(9, Timestamp.from(COMPLETED_AT));
            sql.setTimestamp(10, historicalAt == null ? null : Timestamp.from(historicalAt));
            sql.setString(11, historyReason);
            sql.setTimestamp(12, Timestamp.from(ORIGINAL_UPDATED_AT));
            assertThat(sql.executeUpdate()).isOne();
        }
    }

    private static void insertTask(Connection connection, UUID taskId, UUID parentId,
                                   UUID versionId, UUID itemId, int order) throws Exception {
        try (var sql = connection.prepareStatement("""
                insert into public.checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, template_version_id,
                    template_item_version_id, task_key, key_version, title_snapshot,
                    display_order, is_required, target_subject, checklist_contract_version,
                    status, lock_version, completed_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, 'v1', 'Completed required leaf', ?, false, null, 2,
                    'COMPLETED', 9, ?, now(), ?)
                """)) {
            sql.setObject(1, taskId);
            sql.setObject(2, parentId);
            sql.setObject(3, versionId);
            sql.setObject(4, itemId);
            sql.setString(5, hexKey(taskId));
            sql.setInt(6, order);
            sql.setTimestamp(7, Timestamp.from(COMPLETED_AT));
            sql.setTimestamp(8, Timestamp.from(ORIGINAL_UPDATED_AT));
            assertThat(sql.executeUpdate()).isOne();
        }
    }

    private static Map<UUID, TaskSnapshot> snapshots(DataSource dataSource, Fixture fixture)
            throws Exception {
        Map<UUID, TaskSnapshot> snapshots = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             var sql = connection.prepareStatement("""
                     select checklist_task_instance_id, is_required, status, lock_version,
                            completed_at, skipped_at, cancelled_at, action_reason_code, updated_at
                       from public.checklist_task_instances
                      where checklist_task_instance_id = any (?)
                     """)) {
            var ids = connection.createArrayOf("uuid", fixture.taskIds().toArray());
            sql.setArray(1, ids);
            try (var rows = sql.executeQuery()) {
                while (rows.next()) {
                    UUID id = rows.getObject("checklist_task_instance_id", UUID.class);
                    snapshots.put(id, new TaskSnapshot(
                            rows.getBoolean("is_required"), rows.getString("status"),
                            rows.getLong("lock_version"), instant(rows, "completed_at"),
                            instant(rows, "skipped_at"), instant(rows, "cancelled_at"),
                            rows.getString("action_reason_code"), instant(rows, "updated_at")));
                }
            }
        }
        assertThat(snapshots).hasSize(4);
        return snapshots;
    }

    private static Instant instant(java.sql.ResultSet rows, String column) throws Exception {
        Timestamp value = rows.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static void assertLifecyclePreserved(TaskSnapshot before, TaskSnapshot after) {
        assertThat(after.status()).isEqualTo(before.status());
        assertThat(after.lockVersion()).isEqualTo(before.lockVersion() + 1);
        assertThat(after.completedAt()).isEqualTo(before.completedAt());
        assertThat(after.skippedAt()).isEqualTo(before.skippedAt());
        assertThat(after.cancelledAt()).isEqualTo(before.cancelledAt());
        assertThat(after.actionReasonCode()).isEqualTo(before.actionReasonCode());
        assertThat(after.updatedAt()).isEqualTo(before.updatedAt());
    }

    private static String hexKey(UUID id) {
        return id.toString().replace("-", "") + "0".repeat(32);
    }

    private static Flyway flyway(DataSource dataSource, String target) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .ignoreMigrationPatterns("*:future");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private record Fixture(UUID currentTask, UUID historicalTask, UUID userCreatedTask,
                           UUID nonSequenceTask) {
        private java.util.List<UUID> taskIds() {
            return java.util.List.of(currentTask, historicalTask, userCreatedTask, nonSequenceTask);
        }
    }

    private record TaskSnapshot(boolean required, String status, long lockVersion,
                                Instant completedAt, Instant skippedAt, Instant cancelledAt,
                                String actionReasonCode, Instant updatedAt) {
    }
}
