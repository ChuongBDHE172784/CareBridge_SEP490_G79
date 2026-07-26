package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class ContentReportFlywayBranchHistoryIntegrationTest {

    private static final MigrationVersion PRE_BRIDGE =
            MigrationVersion.fromVersion("20260720100000.5");
    private static final MigrationVersion ORIGINAL =
            MigrationVersion.fromVersion("20260720100001");
    private static final MigrationVersion GITHUB_REPAIR =
            MigrationVersion.fromVersion("20260722020200");
    private static final MigrationVersion POST_BRIDGE =
            MigrationVersion.fromVersion("20260722020250");
    private static final MigrationVersion PHASE2_CUTOVER =
            MigrationVersion.fromVersion("20260722231900");

    private static final String REPORT_WITH_VALUES =
            "72000000-0000-0000-0000-000000000001";
    private static final String REPORT_WITH_NULLS =
            "72000000-0000-0000-0000-000000000002";
    private static final String REVERTED_BY =
            "72000000-0000-0000-0000-000000000011";
    private static final String LEGACY_OWNER =
            "72000000-0000-0000-0000-000000000021";
    private static final String LEGACY_JOURNEY =
            "72000000-0000-0000-0000-000000000022";
    private static final String LEGACY_BABY =
            "72000000-0000-0000-0000-000000000023";
    private static final String DATE_MISMATCH_JOURNEY =
            "72000000-0000-0000-0000-000000000031";
    private static final String DATE_MISMATCH_BABY =
            "72000000-0000-0000-0000-000000000032";
    private static final String UNAPPROVED_OWNER =
            "72000000-0000-0000-0000-000000000041";
    private static final String UNAPPROVED_JOURNEY_OWNER =
            "72000000-0000-0000-0000-000000000042";
    private static final String UNAPPROVED_JOURNEY =
            "72000000-0000-0000-0000-000000000043";
    private static final String UNAPPROVED_BABY =
            "72000000-0000-0000-0000-000000000044";

    private static final Set<String> LOCAL_ONLY_MIGRATIONS = Set.of(
            "V20260720100000_5__bridge_content_report_revert_history.sql",
            "V20260720100001__add_content_report_revert_columns.sql",
            "V20260722019950__bridge_story65_branch_history.sql",
            "V20260722020000__quarantine_invalid_legacy_baby_journey_links.sql",
            "V20260722020100__quarantine_outcome_date_inconsistent_baby_journey_links.sql",
            "V20260722020250__restore_content_report_revert_history.sql",
            "V20260722020450__preserve_story68_handoff_history.sql",
            "V20260722020850__restore_story68_history_references.sql",
            "V20260722119950__bridge_story66_out_of_order_parents.sql",
            "V20260722120000__guarantee_triage_emergency_idempotency.sql",
            "V20260722210000__persist_lifecycle_safety_outcomes_and_continuations.sql",
            "V20260723090000__create_consented_triage_expert_handoffs.sql",
            "V20260722231350__preserve_epic6_lifecycle_bindings.sql",
            "V20260722231360__bridge_story66_safety_state.sql",
            "V20260722231950__bridge_story68_handoff_history.sql");

    private static final Set<String> STORY65_UNRELATED_MIGRATIONS = Set.of(
            "V20260722020450__preserve_story68_handoff_history.sql",
            "V20260722020850__restore_story68_history_references.sql",
            "V20260722119950__bridge_story66_out_of_order_parents.sql",
            "V20260722120000__guarantee_triage_emergency_idempotency.sql",
            "V20260722210000__persist_lifecycle_safety_outcomes_and_continuations.sql",
            "V20260722231350__preserve_epic6_lifecycle_bindings.sql",
            "V20260722231360__bridge_story66_safety_state.sql",
            "V20260722231950__bridge_story68_handoff_history.sql",
            "V20260723090000__create_consented_triage_expert_handoffs.sql",
            "V20260724210000__canonical_safety_action_invariants.sql",
            "V20260724211000__canonical_triage_lifecycle_integrity.sql",
            "V20260724211500__canonical_story68_handoff_integrity.sql");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void resetDatabase() throws Exception {
        execute("DROP SCHEMA IF EXISTS public CASCADE");
        execute("CREATE SCHEMA public");
    }

    @Test
    void flywayDiscoversFractionalBridgeBetweenImmutableVersions() {
        List<MigrationVersion> versions = Arrays.stream(flyway(migrationRoot(), POST_BRIDGE, true)
                        .info()
                        .all())
                .map(MigrationInfo::getVersion)
                .filter(java.util.Objects::nonNull)
                .toList();

        assertThat(PRE_BRIDGE).isGreaterThan(MigrationVersion.fromVersion("20260720100000"));
        assertThat(PRE_BRIDGE).isLessThan(ORIGINAL);
        assertThat(versions.indexOf(PRE_BRIDGE)).isLessThan(versions.indexOf(ORIGINAL));
        assertThat(versions.indexOf(ORIGINAL)).isLessThan(versions.indexOf(GITHUB_REPAIR));
        assertThat(versions.indexOf(GITHUB_REPAIR)).isLessThan(versions.indexOf(POST_BRIDGE));
    }

    @Test
    void cleanHistoryPreservesRowsThatPredateTheRevertColumns() throws Exception {
        migrate(migrationRoot(), MigrationVersion.fromVersion("20260720100000"), false);
        insertReportWithoutRevertColumns(REPORT_WITH_NULLS);

        migrate(migrationRoot(), POST_BRIDGE, false);

        assertReport(REPORT_WITH_NULLS, false);
        assertBridgeRemoved();
    }

    @Test
    void featureHistoryPreservesAppliedOriginalValuesWithoutChecksumRepair() throws Exception {
        Path featureHistory = copyMigrationsThrough(
                tempDirectory.resolve("feature-history"), ORIGINAL, Set.of(
                        "V20260720100000_5__bridge_content_report_revert_history.sql"));
        migrate(featureHistory, ORIGINAL, false);
        insertReportsWithRevertColumns();

        migrate(migrationRoot(), POST_BRIDGE, true);

        assertReport(REPORT_WITH_VALUES, true);
        assertReport(REPORT_WITH_NULLS, false);
        assertBridgeRemoved();
        assertThat(number("SELECT count(*) FROM flyway_schema_history "
                + "WHERE version='20260720100001' AND success")).isOne();
    }

    @Test
    void githubHistoryRunsTheImmutableOriginalOutOfOrderAndPreservesValues() throws Exception {
        Path githubHistory = copyMigrationsThrough(
                tempDirectory.resolve("github-history"), GITHUB_REPAIR, LOCAL_ONLY_MIGRATIONS);
        migrate(githubHistory, GITHUB_REPAIR, false);
        insertReportsWithRevertColumns();

        migrate(migrationRoot(), POST_BRIDGE, true);

        assertReport(REPORT_WITH_VALUES, true);
        assertReport(REPORT_WITH_NULLS, false);
        assertBridgeRemoved();
        assertThat(number("SELECT count(*) FROM flyway_schema_history "
                + "WHERE version IN ('20260720100000.5','20260720100001','20260722020250') "
                + "AND success")).isEqualTo(3);
    }

    @Test
    void resumeAfterPreBridgeCommitRestoresEveryCapturedValue() throws Exception {
        Path featureHistory = copyMigrationsThrough(
                tempDirectory.resolve("resume-feature-history"), ORIGINAL, Set.of(
                        "V20260720100000_5__bridge_content_report_revert_history.sql"));
        migrate(featureHistory, ORIGINAL, false);
        insertReportsWithRevertColumns();

        Path interruptedLocation = copyMigrationsThrough(
                tempDirectory.resolve("interrupted-after-pre"), ORIGINAL, Set.of());
        migrate(interruptedLocation, ORIGINAL, true);

        assertThat(columnExists("content_reports", "reverted_at")).isFalse();
        assertThat(columnExists("content_reports", "reverted_by")).isFalse();
        assertThat(number("SELECT count(*) FROM "
                + "carebridge_migration_bridge.content_report_revert_rows")).isEqualTo(2);

        migrate(migrationRoot(), POST_BRIDGE, true);

        assertReport(REPORT_WITH_VALUES, true);
        assertReport(REPORT_WITH_NULLS, false);
        assertBridgeRemoved();
    }

    @Test
    void githubPhase2CutoverConvergesLocalHistoryWithoutDataOrHistoryRepair() throws Exception {
        Path githubHistory = copyMigrationsThrough(
                tempDirectory.resolve("github-phase2-history"),
                PHASE2_CUTOVER,
                LOCAL_ONLY_MIGRATIONS);
        migrate(githubHistory, GITHUB_REPAIR, false);
        seedGithubLegacyAnomalyAndModerationHistory();
        migrate(githubHistory, PHASE2_CUTOVER, false);

        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id='"
                + LEGACY_BABY + "' AND mother_journey_id='" + LEGACY_JOURNEY + "'"))
                .isOne();
        assertCanonicalModerationHistory();

        migrate(migrationRoot(), MigrationVersion.LATEST, true);

        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id='"
                + LEGACY_BABY + "' AND mother_journey_id IS NULL")).isOne();
        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id='"
                + DATE_MISMATCH_BABY + "' AND mother_journey_id IS NULL")).isOne();
        assertCanonicalModerationHistory();
        assertThat(number("""
                SELECT count(*) FROM audit_events
                 WHERE event_category='DATA_MIGRATION'
                   AND resource_type='baby_journey_link_cleanup_summary'
                   AND after_payload_jsonb::text LIKE '%V20260724212500_CANONICAL%'
                   AND after_payload_jsonb::text LIKE '%MISSING_OUTCOME_EVIDENCE%'
                   AND after_payload_jsonb::text LIKE '%OUTCOME_DATE_MISMATCH%'
                """)).isOne();
        assertThat(number("""
                SELECT count(*)
                  FROM unnest(ARRAY[
                      to_regclass('public.baby_profiles'),
                      to_regclass('public.pregnancy_outcome_evidence'),
                      to_regclass('public.baby_journey_link_cleanup_summary'),
                      to_regclass('carebridge_migration_bridge.story65_branch_history_state')
                  ]::oid[]) relation
                 WHERE relation IS NOT NULL
                """)).isZero();
    }

    @Test
    void githubCutoverAppliesBothApprovedCanonicalStory65Repairs() throws Exception {
        Path githubHistory = copyMigrationsThrough(
                tempDirectory.resolve("github-approved-story65-history"),
                PHASE2_CUTOVER,
                LOCAL_ONLY_MIGRATIONS);
        migrate(githubHistory, GITHUB_REPAIR, false);
        seedGithubLegacyAnomalyAndModerationHistory();
        migrate(githubHistory, PHASE2_CUTOVER, false);

        MigrationVersion archiveTarget = MigrationVersion.fromVersion("20260724213000");
        Path story65Only = copyMigrationsThrough(
                tempDirectory.resolve("story65-approved-merged"),
                archiveTarget,
                STORY65_UNRELATED_MIGRATIONS);
        migrate(story65Only, archiveTarget, true);

        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id IN ('"
                + LEGACY_BABY + "','" + DATE_MISMATCH_BABY
                + "') AND mother_journey_id IS NULL")).isEqualTo(2);
        assertCanonicalModerationHistory();
        assertThat(number("""
                SELECT count(*) FROM audit_events
                 WHERE event_category='DATA_MIGRATION'
                   AND resource_type='baby_journey_link_cleanup_summary'
                   AND after_payload_jsonb::text LIKE '%V20260724212500_CANONICAL%'
                   AND after_payload_jsonb::text LIKE '%MISSING_OUTCOME_EVIDENCE%'
                   AND after_payload_jsonb::text LIKE '%OUTCOME_DATE_MISMATCH%'
                """)).isOne();
        assertThat(number("SELECT count(*) FROM pg_class relation "
                + "JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace "
                + "WHERE (namespace.nspname='public' AND relation.relname IN ("
                + "'baby_profiles','pregnancy_outcome_evidence',"
                + "'baby_journey_link_cleanup_summary')) "
                + "OR (namespace.nspname='carebridge_migration_bridge' "
                + "AND relation.relname='story65_branch_history_state')")).isZero();
    }

    @Test
    void canonicalRepairRejectsUnapprovedOwnerMismatchTransactionally() throws Exception {
        Path githubHistory = copyMigrationsThrough(
                tempDirectory.resolve("github-unapproved-history"),
                PHASE2_CUTOVER,
                LOCAL_ONLY_MIGRATIONS);
        migrate(githubHistory, GITHUB_REPAIR, false);
        seedGithubOwnerMismatch();
        migrate(githubHistory, PHASE2_CUTOVER, false);

        MigrationVersion repairTarget = MigrationVersion.fromVersion("20260724212500");
        Path story65Only = copyMigrationsThrough(
                tempDirectory.resolve("story65-unapproved-merged"),
                repairTarget,
                STORY65_UNRELATED_MIGRATIONS);

        assertThatThrownBy(() -> migrate(story65Only, repairTarget, true))
                .hasStackTraceContaining("canonical Story 6.5 cleanup exceeds approval");

        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id='"
                + UNAPPROVED_BABY + "' AND mother_journey_id='"
                + UNAPPROVED_JOURNEY + "'")).isOne();
        assertThat(number("SELECT count(*) FROM baby_journey_link_cleanup_summary "
                + "WHERE migration_key='V20260724212500_CANONICAL'")).isZero();
    }

    private Path copyMigrationsThrough(
            Path destination, MigrationVersion target, Set<String> excludedNames)
            throws IOException {
        Path source = migrationRoot();
        Set<String> exclusions = new HashSet<>(excludedNames);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path output = destination.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(output);
                    continue;
                }
                String name = path.getFileName().toString();
                MigrationVersion version = migrationVersion(name);
                if (version == null || version.compareTo(target) > 0 || exclusions.contains(name)) {
                    continue;
                }
                Files.createDirectories(output.getParent());
                Files.copy(path, output, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return destination;
    }

    private MigrationVersion migrationVersion(String fileName) {
        if (!fileName.startsWith("V") || !fileName.endsWith(".sql")) {
            return null;
        }
        int descriptionSeparator = fileName.indexOf("__");
        if (descriptionSeparator < 2) {
            return null;
        }
        String rawVersion = fileName.substring(1, descriptionSeparator).replace('_', '.');
        return MigrationVersion.fromVersion(rawVersion);
    }

    private Path migrationRoot() {
        try {
            var resource = Thread.currentThread().getContextClassLoader().getResource("db/migration");
            if (resource == null || !"file".equals(resource.getProtocol())) {
                throw new IllegalStateException("exploded db/migration test resource is required");
            }
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("invalid migration resource path", exception);
        }
    }

    private void migrate(Path location, MigrationVersion target, boolean outOfOrder) {
        flyway(location, target, outOfOrder).migrate();
    }

    private Flyway flyway(Path location, MigrationVersion target, boolean outOfOrder) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:" + location.toAbsolutePath().toString().replace('\\', '/'))
                .target(target)
                .outOfOrder(outOfOrder)
                .validateOnMigrate(true)
                .load();
    }

    private void insertReportWithoutRevertColumns(String reportId) throws Exception {
        execute("INSERT INTO content_reports(report_id,created_at,status) VALUES ('"
                + reportId + "',now(),'PENDING')");
    }

    private void insertReportsWithRevertColumns() throws Exception {
        execute("""
                INSERT INTO content_reports(
                    report_id,created_at,status,reverted_at,reverted_by)
                VALUES
                    ('72000000-0000-0000-0000-000000000001',now(),'PENDING',
                     '2026-07-20T10:15:30Z','72000000-0000-0000-0000-000000000011'),
                    ('72000000-0000-0000-0000-000000000002',now(),'PENDING',NULL,NULL)
                """);
    }

    private void seedGithubLegacyAnomalyAndModerationHistory() throws Exception {
        execute("""
                INSERT INTO users(
                    user_id,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES
                    ('72000000-0000-0000-0000-000000000021',
                     'github.cutover@test','MOTHER','ACTIVE',true,false,true,false,now(),now()),
                    ('72000000-0000-0000-0000-000000000030',
                     'date.mismatch@test','MOTHER','ACTIVE',true,false,true,false,now(),now());
                INSERT INTO mother_journeys(
                    journey_id,owner_user_id,journey_type,status,pregnancy_outcome,
                    pregnancy_outcome_date,created_at,updated_at)
                VALUES
                    ('72000000-0000-0000-0000-000000000022',
                     '72000000-0000-0000-0000-000000000021','POSTPARTUM','ACTIVE',
                     'LIVE_BIRTH','2026-07-01',now(),now()),
                    ('72000000-0000-0000-0000-000000000031',
                     '72000000-0000-0000-0000-000000000030','POSTPARTUM','ACTIVE',
                     'LIVE_BIRTH','2026-07-02',now(),now());
                INSERT INTO baby_profiles(
                    baby_id,owner_user_id,nickname,status,is_active,related_journey_id,
                    created_at,updated_at)
                VALUES
                    ('72000000-0000-0000-0000-000000000023',
                     '72000000-0000-0000-0000-000000000021','Cutover Baby','ACTIVE',true,
                     '72000000-0000-0000-0000-000000000022',now(),now()),
                    ('72000000-0000-0000-0000-000000000032',
                     '72000000-0000-0000-0000-000000000030','Date Mismatch Baby','ACTIVE',true,
                     '72000000-0000-0000-0000-000000000031',now(),now());
                INSERT INTO pregnancy_outcome_evidence(
                    evidence_id,journey_id,owner_user_id,submission_id,outcome_type,
                    outcome_date,source,actor_user_id,reason,effective_at,revision_number,
                    journey_version,semantic_hash,correction)
                VALUES ('72000000-0000-0000-0000-000000000033',
                    '72000000-0000-0000-0000-000000000031',
                    '72000000-0000-0000-0000-000000000030',
                    '72000000-0000-0000-0000-000000000034','LIVE_BIRTH','2026-07-01',
                    'SELF_REPORTED','72000000-0000-0000-0000-000000000030',
                    'date mismatch fixture',now(),1,1,'date-mismatch-hash',false);
                INSERT INTO content_reports(
                    report_id,target_id,target_type,created_at,status,reverted_at,reverted_by)
                VALUES ('72000000-0000-0000-0000-000000000001',
                    '72000000-0000-0000-0000-000000000024','CONTENT',now(),'PENDING',
                    '2026-07-20T10:15:30Z','72000000-0000-0000-0000-000000000011');
                """);
    }

    private void seedGithubOwnerMismatch() throws Exception {
        execute("""
                INSERT INTO users(
                    user_id,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES
                    ('72000000-0000-0000-0000-000000000041',
                     'unapproved.owner@test','MOTHER','ACTIVE',true,false,true,false,now(),now()),
                    ('72000000-0000-0000-0000-000000000042',
                     'journey.owner@test','MOTHER','ACTIVE',true,false,true,false,now(),now());
                INSERT INTO mother_journeys(
                    journey_id,owner_user_id,journey_type,status,pregnancy_outcome,
                    pregnancy_outcome_date,created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000043',
                    '72000000-0000-0000-0000-000000000042','POSTPARTUM','ACTIVE',
                    'LIVE_BIRTH','2026-07-01',now(),now());
                INSERT INTO baby_profiles(
                    baby_id,owner_user_id,nickname,status,is_active,related_journey_id,
                    created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000044',
                    '72000000-0000-0000-0000-000000000041','Owner Mismatch Baby',
                    'ACTIVE',true,'72000000-0000-0000-0000-000000000043',now(),now());
                """);
    }

    private void assertCanonicalModerationHistory() throws Exception {
        assertThat(number("SELECT count(*) FROM moderation_cases WHERE moderation_case_id='"
                + REPORT_WITH_VALUES
                + "' AND reverted_at='2026-07-20T10:15:30Z'::timestamptz "
                + "AND reverted_by='" + REVERTED_BY + "'"))
                .isOne();
    }

    private void assertReport(String reportId, boolean hasValues) throws Exception {
        String predicate = hasValues
                ? "reverted_at='2026-07-20T10:15:30Z'::timestamptz AND reverted_by='" + REVERTED_BY + "'"
                : "reverted_at IS NULL AND reverted_by IS NULL";
        assertThat(number("SELECT count(*) FROM content_reports WHERE report_id='"
                + reportId + "' AND " + predicate)).isOne();
    }

    private void assertBridgeRemoved() throws Exception {
        assertThat(number("SELECT count(*) FROM pg_class relation "
                + "JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace "
                + "WHERE namespace.nspname='carebridge_migration_bridge' "
                + "AND relation.relname IN ('content_report_revert_state',"
                + "'content_report_revert_rows')")).isZero();
    }

    private boolean columnExists(String table, String column) throws Exception {
        return number("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema='public' AND table_name='" + table
                + "' AND column_name='" + column + "'") == 1;
    }

    private long number(String sql) throws Exception {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
