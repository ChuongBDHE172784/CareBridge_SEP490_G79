package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Focused real-PostgreSQL validation for the test-only synthetic fixture and correction. */
@EnabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named = "checklist.synthetic.fixture.validation.enabled", matches = "true")
class ChecklistSyntheticFixtureEmbeddedPostgresTest {

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void normalChainLeavesOnlyTheTwoReviewedInvalidRows() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30)).setServerConfig("max_connections", "100").start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            assertThat(flyway(dataSource, false, "20260728010000").migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                executeSection(connection, "-- CHK041:PRE_EXPAND", "-- CHK041:POST_EXPAND");
            }
            assertThat(flyway(dataSource, false, "20260729060000").migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                executeSection(connection, "-- CHK041:POST_EXPAND", "-- CHK041:POST_EXPAND_END");
            }
            assertThat(flyway(dataSource, false, null).migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                assertThat(count(connection, "select count(*) from preparation_checklist_items")).isEqualTo(10_002);
                assertThat(count(connection, """
                        select count(*) from preparation_checklist_items source
                        join checklist_task_instances target
                          on target.checklist_task_instance_id=source.checklist_item_id
                        where source.checklist_item_id::text between
                          'c0414000-0000-4000-8000-000000000001' and
                          'c0414000-0000-4000-8000-000000010000'
                        """)).isEqualTo(10_000);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine where resolved_at is null
                        """)).isEqualTo(2);
                assertThat(count(connection, """
                        select count(*) from care_tasks
                        where task_id::text like 'c0419000-0000-4000-8000-%'
                          and assignee_user_id='10000000-0000-0000-0000-000000000006'
                          and status='OPEN'
                          and ((journey_id is not null and baby_id is null and target_subject='MOTHER')
                            or (journey_id is null and baby_id is not null and target_subject='BABY'))
                        """)).isEqualTo(500);
                assertThat(count(connection, """
                        select count(*) from care_group_members
                        where care_group_member_id::text like 'c0413200-0000-4000-8000-%'
                          and user_id='10000000-0000-0000-0000-000000000006'
                          and invitation_status='ACCEPTED'
                          and permission_json->>'CHECKLIST_VIEW'='true'
                          and permission_json->>'CHECKLIST_COMPLETE'='true'
                        """)).isEqualTo(20);
                assertThat(count(connection, """
                        select count(*) from checklist_instances
                        where recipient_user_id='10000000-0000-0000-0000-000000000006'
                        """)).isZero();
            }
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void baseChainAbortsOnControlledDualOutcomeAndCorrectionLeavesReviewedBaseline() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30)).setServerConfig("max_connections", "100").start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            assertThat(flyway(dataSource, false, "20260728010000").migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                executeSection(connection, "-- CHK041:PRE_EXPAND", "-- CHK041:POST_EXPAND");
            }
            assertThat(flyway(dataSource, false, "20260729060000").migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                executeSection(connection, "-- CHK041:POST_EXPAND", "-- CHK041:POST_EXPAND_END");
                executeSection(connection, "-- CHK041:CHALLENGE", "-- CHK041:END");
            }
            assertThat(flyway(dataSource, false, null).migrate().success).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                try (var statement = connection.createStatement(); var reasons = statement.executeQuery("""
                        select source_id, reason_code from checklist_migration_quarantine
                        where resolved_at is null order by source_id, reason_code
                        """)) {
                    while (reasons.next()) {
                        System.out.printf("CHK-041 fixture quarantine source=%s reason=%s%n",
                                reasons.getString(1), reasons.getString(2));
                    }
                }
                assertThat(count(connection, "select count(*) from preparation_checklist_items")).isEqualTo(10_002);
                assertThat(count(connection, """
                        select count(*) from preparation_checklist_items source
                        join checklist_task_instances target
                          on target.checklist_task_instance_id=source.checklist_item_id
                        where source.checklist_item_id::text between
                          'c0414000-0000-4000-8000-000000000001' and
                          'c0414000-0000-4000-8000-000000010000'
                        """)).isEqualTo(10_000);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine where resolved_at is null
                        """)).isEqualTo(3);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine
                        where resolved_at is null and reason_code='AMBIGUOUS_LEGACY_CONTEXT'
                        """)).isEqualTo(1);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine
                        where resolved_at is null and reason_code='UNKNOWN_LEGACY_CONTEXT'
                        """)).isEqualTo(1);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine
                        where resolved_at is null and reason_code='LEGACY_TASK_KEY_COLLISION'
                        """)).isEqualTo(1);
            }
            Flyway corrected = flyway(dataSource, true, null);
            assertThat(corrected.migrate().success).isTrue();
            assertThat(corrected.validateWithResult().validationSuccessful).isTrue();
            try (Connection connection = dataSource.getConnection()) {
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine where resolved_at is null
                        """)).isEqualTo(2);
                assertThat(count(connection, """
                        select count(*) from checklist_migration_quarantine
                        where resolved_at is null and reason_code='LEGACY_TASK_KEY_COLLISION'
                        """)).isZero();
            }
        }
    }

    private static Flyway flyway(javax.sql.DataSource dataSource, boolean correction, String target) {
        var configuration = Flyway.configure().dataSource(dataSource).locations(correction
                ? new String[] {"classpath:db/migration-legacy", "classpath:checklist/correction"}
                : new String[] {"classpath:db/migration-legacy"});
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private static void executeSection(Connection connection, String startMarker, String endMarker) throws Exception {
        String fixture;
        try (var stream = ChecklistSyntheticFixtureEmbeddedPostgresTest.class.getResourceAsStream(
                "/checklist/chk041-production-representative-fixture.sql")) {
            assertThat(stream).isNotNull();
            fixture = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        int start = fixture.indexOf(startMarker);
        int end = fixture.indexOf(endMarker, start + startMarker.length());
        String section = fixture.substring(start + startMarker.length(), end);
        ScriptUtils.executeSqlScript(connection,
                new InputStreamResource(new ByteArrayInputStream(section.getBytes(StandardCharsets.UTF_8))));
    }

    private static long count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
