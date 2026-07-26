package com.carebridge.backend.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalWardMigrationIntegrationTest {

    private static final MigrationVersion PRE_WARD_FORWARD =
            MigrationVersion.fromVersion("20260724213000");
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_SQL = Pattern.compile("^V(.+)__.+\\.sql$");

    private static final Set<String> APPROVED_TABLES = Set.of(
            "persons", "care_subjects", "users", "user_identities", "auth_sessions",
            "auth_revocations", "auth_challenges", "account_deletion_requests",
            "mother_journeys", "mother_journey_events", "maternal_observations",
            "maternal_exercise_sessions", "care_logs", "growth_measurements",
            "development_milestones", "vaccination_records", "vaccination_schedules",
            "community_profiles", "community_topics", "community_content",
            "community_interactions", "professional_profiles", "specialties",
            "professional_specialties", "expert_credentials", "expert_availability",
            "expert_location_shares", "expert_contribution_events", "triage_sessions",
            "triage_session_evidence", "red_flag_rules", "health_context_memories",
            "knowledge_sources", "knowledge_source_reviews", "health_records", "attachments",
            "health_record_attachments", "device_connections", "health_observations",
            "care_groups", "care_group_members", "scheduled_care_items", "family_tasks",
            "preparation_checklist_items", "care_item_templates", "content_items",
            "content_item_topics", "content_item_sources", "moderation_cases",
            "moderation_events", "notification_records", "device_tokens", "safety_configs",
            "safety_monitoring_sessions", "safety_events", "safety_event_actions",
            "emergency_contacts", "administrative_areas", "care_facilities",
            "nearby_support_requests", "nearby_support_responses", "audit_events",
            "security_events", "data_permissions", "system_configurations", "expense_entries",
            "archived_consultation_records", "archived_realtime_records",
            "archived_partner_records", "flyway_schema_history",
            "expert_consultation_requests", "consultation_context_shares",
            "consultation_context_citations");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA IF EXISTS carebridge_migration_bridge CASCADE");
        execute("DROP SCHEMA public CASCADE");
        execute("CREATE SCHEMA public");
    }

    @Test
    void forwardMigrationPreservesCanonicalIdentityAndEndsWithExactlySeventyPlusThreeTables()
            throws Exception {
        migrateTo(PRE_WARD_FORWARD);

        String districtIdentity = scalar("""
                SELECT administrative_area_id::text || '|' || parent_area_id::text
                  FROM administrative_areas
                 WHERE code='DISTRICT:0101'
                """);
        execute("UPDATE administrative_areas SET name='stale seed value' WHERE code='DISTRICT:0101'");

        migrateTo(null);

        assertThat(tableNames()).containsExactlyInAnyOrderElementsOf(APPROVED_TABLES);
        assertThat(tableNames()).hasSize(73);
        assertThat(scalar("SELECT to_regclass('public.wards') IS NULL")).isEqualTo("t");
        assertThat(scalar("""
                SELECT administrative_area_id::text || '|' || parent_area_id::text
                  FROM administrative_areas
                 WHERE code='DISTRICT:0101'
                """)).isEqualTo(districtIdentity);
        assertThat(scalar("SELECT name FROM administrative_areas WHERE code='DISTRICT:0101'"))
                .isEqualTo("Ba Đình");
        assertThat(scalar("SELECT name_en FROM administrative_areas WHERE code='DISTRICT:0101'"))
                .isEqualTo("Ba Dinh");
        assertThat(scalar("""
                SELECT string_agg(area_type || ':' || area_count, ',' ORDER BY area_type)
                  FROM (
                        SELECT area_type, count(*) AS area_count
                          FROM administrative_areas
                         GROUP BY area_type
                       ) counts
                """)).isEqualTo("DISTRICT:403,PROVINCE:36,WARD:35");
        assertThat(scalar("""
                SELECT count(*) FROM administrative_areas
                 WHERE area_type='DISTRICT' AND length(legacy_code)=4
                """)).isEqualTo("399");
        assertThat(scalar("SELECT count(*) FROM administrative_areas WHERE area_type='WARD'"))
                .isEqualTo("35");
        assertThat(scalar("""
                SELECT ward.legacy_code || '|' || district.legacy_code || '|' ||
                       province.legacy_code || '|' || ward.name || '|' || ward.name_en
                  FROM administrative_areas ward
                  JOIN administrative_areas district
                    ON district.administrative_area_id=ward.parent_area_id
                   AND district.area_type='DISTRICT'
                  JOIN administrative_areas province
                    ON province.administrative_area_id=district.parent_area_id
                   AND province.area_type='PROVINCE'
                 WHERE ward.code='WARD:01001' AND ward.area_type='WARD'
                """)).isEqualTo("01001|0101|01|Phúc Xá|Phuc Xa");
        assertThat(scalar("""
                SELECT count(*) FROM flyway_schema_history
                 WHERE success AND version='20260724214000'
                   AND script='V20260724214000__canonicalize_administrative_area_wards.sql'
                """)).isEqualTo("1");
    }

    @Test
    void repositoryHasUniqueVersionsAndNoRejectedBackdatedMigrationFiles() throws Exception {
        Map<String, List<String>> scriptsByVersion = new TreeMap<>();
        List<String> scripts = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(MIGRATIONS)) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .forEach(name -> {
                        scripts.add(name);
                        var matcher = VERSIONED_SQL.matcher(name);
                        assertThat(matcher.matches()).as("migration filename %s", name).isTrue();
                        String version = MigrationVersion.fromVersion(matcher.group(1)).getVersion();
                        scriptsByVersion.computeIfAbsent(version, ignored -> new ArrayList<>())
                                .add(name);
                    });
        }

        assertThat(scriptsByVersion).allSatisfy((version, versionScripts) ->
                assertThat(versionScripts)
                        .as("Flyway version %s must be unique", version)
                        .hasSize(1));
        assertThat(scripts)
                .doesNotContain(
                        "V20260721000002__full_vietnam_districts_wards_2025.sql",
                        "V20260722000000__create_wards_table.sql")
                .contains(
                        "V20260721000003__fix_expert_profiles_experience_column.sql",
                        "V20260724214000__canonicalize_administrative_area_wards.sql");
    }

    private static void migrateTo(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private static List<String> tableNames() throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT table_name
                          FROM information_schema.tables
                         WHERE table_schema='public' AND table_type='BASE TABLE'
                         ORDER BY table_name
                        """)) {
            List<String> names = new ArrayList<>();
            while (result.next()) {
                names.add(result.getString(1));
            }
            return names;
        }
    }

    private static void execute(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(String sql) throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
