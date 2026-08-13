package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Exercises the new migration against rows that existed before its columns. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistItemDetailsMigrationEmbeddedPostgresTest {

    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration",
            "V20260809110000__add_checklist_item_details_and_support_function.sql");
    private static final Path WHITELIST_EXTENSION_MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration",
            "V20260813110000__extend_checklist_support_function_whitelist.sql");
    private static final Path MATERNAL_EXERCISES_WHITELIST_EXTENSION_MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration",
            "V20260813120000__extend_checklist_support_function_whitelist_for_maternal_exercises.sql");

    @Test
    @Timeout(90)
    void backfillsDescriptionSnapshotForExistingDistributedTasks() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            UUID itemId = UUID.randomUUID();
            UUID emptyItemId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID emptyTaskId = UUID.randomUUID();
            UUID untemplatedTaskId = UUID.randomUUID();

            createPreMigrationTables(dataSource);
            insertItem(dataSource, itemId, "Đọc hướng dẫn trước khi bắt đầu.");
            insertItem(dataSource, emptyItemId, null);
            insertTask(dataSource, taskId, itemId);
            insertTask(dataSource, emptyTaskId, emptyItemId);
            insertTask(dataSource, untemplatedTaskId, null);

            executeMigration(dataSource, MIGRATION);
            executeMigration(dataSource, WHITELIST_EXTENSION_MIGRATION);
            executeMigration(dataSource, MATERNAL_EXERCISES_WHITELIST_EXTENSION_MIGRATION);

            assertThat(descriptionSnapshot(dataSource, taskId))
                    .isEqualTo("Đọc hướng dẫn trước khi bắt đầu.");
            assertThat(descriptionSnapshot(dataSource, emptyTaskId)).isNull();
            assertThat(descriptionSnapshot(dataSource, untemplatedTaskId)).isNull();
            assertThat(insertMaternalHealthMetricItem(dataSource)).isOne();
            assertThat(insertMaternalHealthMetricTask(dataSource)).isOne();
            assertThat(insertMaternalExerciseItem(dataSource)).isOne();
            assertThat(insertMaternalExerciseTask(dataSource)).isOne();
        }
    }

    private static void createPreMigrationTables(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("""
                    create table public.care_item_templates (
                        template_id uuid primary key,
                        description text
                    )
                    """);
            statement.execute("""
                    create table public.checklist_task_instances (
                        checklist_task_instance_id uuid primary key,
                        template_item_version_id uuid
                    )
                    """);
        }
    }

    private static void insertItem(DataSource dataSource, UUID id, String description)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into care_item_templates (template_id, description) values (?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, description);
            statement.executeUpdate();
        }
    }

    private static void insertTask(DataSource dataSource, UUID id, UUID itemId)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into checklist_task_instances "
                                + "(checklist_task_instance_id, template_item_version_id) values (?, ?)")) {
            statement.setObject(1, id);
            statement.setObject(2, itemId);
            statement.executeUpdate();
        }
    }

    private static void executeMigration(DataSource dataSource, Path migration) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(Files.readString(migration));
        }
    }

    private static int insertMaternalHealthMetricItem(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into care_item_templates (template_id, support_function_code) "
                                + "values (?, 'MATERNAL_HEALTH_METRICS')")) {
            statement.setObject(1, UUID.randomUUID());
            return statement.executeUpdate();
        }
    }

    private static int insertMaternalHealthMetricTask(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into checklist_task_instances "
                                + "(checklist_task_instance_id, support_function_code) "
                                + "values (?, 'MATERNAL_HEALTH_METRICS')")) {
            statement.setObject(1, UUID.randomUUID());
            return statement.executeUpdate();
        }
    }

    private static int insertMaternalExerciseItem(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into care_item_templates (template_id, support_function_code) "
                                + "values (?, 'MATERNAL_EXERCISES')")) {
            statement.setObject(1, UUID.randomUUID());
            return statement.executeUpdate();
        }
    }

    private static int insertMaternalExerciseTask(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "insert into checklist_task_instances "
                                + "(checklist_task_instance_id, support_function_code) "
                                + "values (?, 'MATERNAL_EXERCISES')")) {
            statement.setObject(1, UUID.randomUUID());
            return statement.executeUpdate();
        }
    }

    private static String descriptionSnapshot(DataSource dataSource, UUID taskId)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "select description_snapshot from checklist_task_instances "
                                + "where checklist_task_instance_id=?")) {
            statement.setObject(1, taskId);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        }
    }
}
