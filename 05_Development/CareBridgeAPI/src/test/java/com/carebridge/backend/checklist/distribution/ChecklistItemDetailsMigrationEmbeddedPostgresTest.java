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

            executeMigration(dataSource);

            assertThat(descriptionSnapshot(dataSource, taskId))
                    .isEqualTo("Đọc hướng dẫn trước khi bắt đầu.");
            assertThat(descriptionSnapshot(dataSource, emptyTaskId)).isNull();
            assertThat(descriptionSnapshot(dataSource, untemplatedTaskId)).isNull();
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

    private static void executeMigration(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(Files.readString(MIGRATION));
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
