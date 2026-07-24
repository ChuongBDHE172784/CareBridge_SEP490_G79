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
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class Story68FlywayBranchHistoryIntegrationTest {

    private static final MigrationVersion PRE_BRIDGE =
            MigrationVersion.fromVersion("20260722020400");
    private static final MigrationVersion HISTORICAL_STORY_68 =
            MigrationVersion.fromVersion("20260723090000");
    private static final MigrationVersion PHASE2_CUTOVER =
            MigrationVersion.fromVersion("20260722231900");
    private static final MigrationVersion CANONICAL_STORY_68 =
            MigrationVersion.fromVersion("20260724211500");

    private static final Set<String> HISTORICAL_FILES = Set.of(
            "V20260722120000__guarantee_triage_emergency_idempotency.sql",
            "V20260722210000__persist_lifecycle_safety_outcomes_and_continuations.sql",
            "V20260723090000__create_consented_triage_expert_handoffs.sql");

    private static final Set<String> GITHUB_EXCLUSIONS = Set.of(
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
            "V20260722231350__preserve_epic6_lifecycle_bindings.sql",
            "V20260722231360__bridge_story66_safety_state.sql",
            "V20260722231950__bridge_story68_handoff_history.sql",
            "V20260723090000__create_consented_triage_expert_handoffs.sql");

    private static final String OWNER = "68000000-0000-0000-0000-000000000001";
    private static final String EXPERT = "68000000-0000-0000-0000-000000000002";
    private static final String JOURNEY = "68000000-0000-0000-0000-000000000011";
    private static final String TRIAGE = "68000000-0000-0000-0000-000000000012";
    private static final String EXPERT_PROFILE = "68000000-0000-0000-0000-000000000013";
    private static final String REQUEST = "68000000-0000-0000-0000-000000000014";
    private static final String IDEMPOTENCY = "68000000-0000-0000-0000-000000000015";
    private static final String SOURCE = "68000000-0000-0000-0000-000000000016";
    private static final String SHARE = "68000000-0000-0000-0000-000000000017";
    private static final String CITATION = "68000000-0000-0000-0000-000000000018";
    private static final String NOTIFICATION = "68000000-0000-0000-0000-000000000019";
    private static final String AUDIT = "68000000-0000-0000-0000-000000000020";
    private static final String CONVERSATION = "68000000-0000-0000-0000-000000000022";
    private static final String WRONG_ARCHIVE = "68000000-0000-0000-0000-000000000023";

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
    void cleanBootstrapRunsImmutableStory68ThroughCanonicalParents() {
        migrate(migrationRoot(), CANONICAL_STORY_68, false, true);

        assertCanonicalStory68Shape();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version="
                + "'20260723090000' AND success")).isOne();
    }

    @Test
    void appliedHistoricalStory68GraphSurvivesOutOfOrderMerge() throws Exception {
        migrate(migrationRoot(), PRE_BRIDGE, false, true);
        seedHistoricalParentsAndReferences();

        Path historical = copyNamedMigrations(
                tempDirectory.resolve("historical-story68"), HISTORICAL_FILES);
        migrate(historical, HISTORICAL_STORY_68, false, false);
        seedHistoricalHandoffGraph();

        migrate(migrationRoot(), CANONICAL_STORY_68, true, true);

        assertCanonicalStory68Shape();
        assertThat(number("SELECT count(*) FROM expert_consultation_requests WHERE id='"
                + REQUEST + "' AND requester_user_id='" + OWNER + "' "
                + "AND expert_profile_id='" + EXPERT_PROFILE + "' "
                + "AND client_request_id='" + IDEMPOTENCY + "' "
                + "AND topic='Historical follow-up' AND status='PENDING' "
                + "AND direct_conversation_id='" + CONVERSATION + "'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM consultation_context_shares WHERE "
                + "context_share_id='" + SHARE + "' AND intake_session_id='" + TRIAGE + "' "
                + "AND consent_grant_id=68001 AND risk_summary='Preserve exact summary'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM consultation_context_citations WHERE "
                + "citation_snapshot_id='" + CITATION + "' AND evidence_source_id='"
                + SOURCE + "' AND ordinal=3"))
                .isOne();
        assertThat(number("SELECT count(*) FROM notification_records WHERE id='"
                + NOTIFICATION + "' AND reference_type='CONSULTATION_REQUEST' "
                + "AND status='SENT' AND attempt_count=3 "
                + "AND metadata->>'payload'='preserve-notification'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM audit_events WHERE audit_event_id='"
                + AUDIT + "' AND resource_type='CONSULTATION_REQUEST' "
                + "AND after_payload_jsonb->>'payload'='preserve-audit' "
                + "AND event_origin='AUDIT_LOG'"))
                .isOne();

        execute("""
                INSERT INTO archived_realtime_records(
                    archive_id,legacy_table,legacy_id,owner_user_id,payload_jsonb,
                    original_created_at,archive_reason,source_schema_version,checksum,
                    conversation_id,sender_user_id,client_message_id,message_type,message_body)
                VALUES ('68000000-0000-0000-0000-000000000023','direct_messages',
                    '68000000-0000-0000-0000-000000000023',
                    '68000000-0000-0000-0000-000000000001','{}',now(),
                    'TEST_WRONG_SUBTYPE','TEST',md5('{}'),
                    '68000000-0000-0000-0000-000000000022',
                    '68000000-0000-0000-0000-000000000001',gen_random_uuid(),
                    'TEXT','wrong subtype')
                """);
        assertThatThrownBy(() -> execute("""
                INSERT INTO expert_consultation_requests(
                    id,requester_user_id,expert_profile_id,client_request_id,topic,
                    description,status,direct_conversation_id,expires_at,created_at,updated_at)
                VALUES (gen_random_uuid(),'68000000-0000-0000-0000-000000000001',
                    '68000000-0000-0000-0000-000000000013',gen_random_uuid(),
                    'Wrong archive','Must reject','PENDING',
                    '68000000-0000-0000-0000-000000000023',
                    now()+interval '1 day',now(),now())
                """)).hasMessageContaining("STORY68_DIRECT_CONVERSATION_SOURCE_MISMATCH");
    }

    @Test
    void githubPhase2CutoverDiscoversHistoricalStory68OutOfOrder() throws Exception {
        Path github = copyMigrationsThrough(
                tempDirectory.resolve("github-story68"),
                PHASE2_CUTOVER,
                GITHUB_EXCLUSIONS);
        migrate(github, PHASE2_CUTOVER, false, true);

        migrate(migrationRoot(), CANONICAL_STORY_68, true, true);

        assertCanonicalStory68Shape();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version IN ("
                + "'20260722020450','20260722020850','20260722120000',"
                + "'20260722210000','20260722231350','20260722231360',"
                + "'20260722231950','20260723090000','20260724211500') AND success"))
                .isEqualTo(9);
    }

    private void seedHistoricalParentsAndReferences() throws Exception {
        execute("""
                INSERT INTO users(user_id,email,role,account_status,enabled,locked,
                                  email_verified,phone_verified,created_at,updated_at)
                VALUES
                  ('68000000-0000-0000-0000-000000000001','story68.owner@test','MOTHER',
                   'ACTIVE',true,false,true,false,now(),now()),
                  ('68000000-0000-0000-0000-000000000002','story68.expert@test','EXPERT',
                   'ACTIVE',true,false,true,false,now(),now());
                INSERT INTO mother_journeys(
                    journey_id,owner_user_id,journey_type,status,created_at,updated_at)
                VALUES ('68000000-0000-0000-0000-000000000011',
                    '68000000-0000-0000-0000-000000000001',
                    'PREGNANCY','ACTIVE',now(),now());
                INSERT INTO intake_sessions(
                    id,user_id,stage,symptoms,risk_level,status,disclaimer,
                    created_at,completed_at,created_by)
                VALUES ('68000000-0000-0000-0000-000000000012',
                    '68000000-0000-0000-0000-000000000001','PREGNANCY',
                    'historical yellow','YELLOW','COMPLETED','fixture',now(),now(),
                    '68000000-0000-0000-0000-000000000001');
                INSERT INTO expert_profiles(
                    expert_profile_id,user_id,specialty,professional_title,
                    experience_years,workplace,verification_status,trust_status,
                    rating_avg,created_at,updated_at)
                VALUES ('68000000-0000-0000-0000-000000000013',
                    '68000000-0000-0000-0000-000000000002','Sản khoa','Bác sĩ',8,
                    'CareBridge','APPROVED','ACTIVE',4.9,now(),now());
                INSERT INTO consent_grants(
                    id,consent_given_at,created_at,data_type,expiry_at,purpose,recipient,
                    revoked_at,scope_text,updated_at,user_id,version,policy_version,
                    evidence_key,locale)
                VALUES (68001,now(),now(),'EXPERT_SHARED_DATA',now()+interval '30 days',
                    'SHARE','68000000-0000-0000-0000-000000000013',NULL,
                    'YELLOW triage context',now(),
                    '68000000-0000-0000-0000-000000000001',1,
                    'YELLOW_EXPERT_CONTEXT_V1',
                    '68000000-0000-0000-0000-000000000015','vi');
                INSERT INTO evidence_sources(
                    id,domain,base_url,organization,category,status,discovery_mode,
                    applicable_stages,added_by,reviewed_by,reviewed_at,notes,
                    created_at,updated_at)
                VALUES ('68000000-0000-0000-0000-000000000016','story68.example.org',
                    'https://story68.example.org','Story 68 Authority','GOVERNMENT',
                    'APPROVED','MANUAL_ADMIN_ADD','PREGNANCY',
                    '68000000-0000-0000-0000-000000000001',
                    '68000000-0000-0000-0000-000000000001',now(),
                    'historical source',now(),now());
                INSERT INTO direct_conversations(
                    conversation_id,mother_user_id,expert_user_id,status,
                    created_at,last_activity_at)
                VALUES ('68000000-0000-0000-0000-000000000022',
                    '68000000-0000-0000-0000-000000000001',
                    '68000000-0000-0000-0000-000000000002','ACTIVE',now(),now());
                INSERT INTO consultation_requests(
                    id,requester_user_id,expert_profile_id,client_request_id,topic,
                    description,status,direct_conversation_id,expires_at,created_at,updated_at)
                VALUES ('68000000-0000-0000-0000-000000000014',
                    '68000000-0000-0000-0000-000000000001',
                    '68000000-0000-0000-0000-000000000013',
                    '68000000-0000-0000-0000-000000000015','Historical follow-up',
                    'Preserve request fields','PENDING',
                    '68000000-0000-0000-0000-000000000022',
                    now()+interval '7 days',now(),now());
                INSERT INTO notification_records(
                    id,user_id,type,title,body,reference_id,reference_type,status,
                    attempt_count,channel,is_read,metadata,created_at,updated_at)
                VALUES ('68000000-0000-0000-0000-000000000019',
                    '68000000-0000-0000-0000-000000000001','CONSULTATION',
                    'Historical request','Preserve notification',
                    '68000000-0000-0000-0000-000000000014','CONSULTATION_REQUEST',
                    'SENT',3,'PUSH',false,
                    '{"eventType":"REQUEST_CREATED","payload":"preserve-notification"}',
                    now(),now());
                INSERT INTO audit_logs(
                    audit_log_id,action,actor_user_id,created_at,entity_id,entity_type,
                    ip_address,new_value_json)
                VALUES ('68000000-0000-0000-0000-000000000020','AI_TRIAGE',
                    '68000000-0000-0000-0000-000000000001',now(),
                    '68000000-0000-0000-0000-000000000014','CONSULTATION_REQUEST',
                    '127.0.0.1','{"payload":"preserve-audit"}');
                """);
    }

    private void seedHistoricalHandoffGraph() throws Exception {
        execute("""
                UPDATE intake_sessions
                   SET journey_id='68000000-0000-0000-0000-000000000011',
                       origin_dashboard='MOTHER_JOURNEY',
                       origin_reference_id='68000000-0000-0000-0000-000000000011',
                       continuation_token='68000000-0000-0000-0000-000000000021',
                       continuation_expires_at=now()+interval '7 days'
                 WHERE id='68000000-0000-0000-0000-000000000012';
                INSERT INTO consultation_context_shares(
                    context_share_id,consultation_request_id,owner_user_id,
                    intake_session_id,expert_profile_id,consent_grant_id,
                    idempotency_key,journey_id,origin_dashboard,origin_reference_id,
                    triage_stage,risk_level,intake_status,risk_summary,
                    share_policy_version,created_at)
                VALUES ('68000000-0000-0000-0000-000000000017',
                    '68000000-0000-0000-0000-000000000014',
                    '68000000-0000-0000-0000-000000000001',
                    '68000000-0000-0000-0000-000000000012',
                    '68000000-0000-0000-0000-000000000013',68001,
                    '68000000-0000-0000-0000-000000000015',
                    '68000000-0000-0000-0000-000000000011','MOTHER_JOURNEY',
                    '68000000-0000-0000-0000-000000000011','PREGNANCY','YELLOW',
                    'COMPLETED','Preserve exact summary','YELLOW_EXPERT_CONTEXT_V1',now());
                INSERT INTO consultation_context_citations(
                    citation_snapshot_id,context_share_id,evidence_source_id,
                    organization,source_url,source_status_at_share,reviewed_at,
                    ordinal,created_at)
                VALUES ('68000000-0000-0000-0000-000000000018',
                    '68000000-0000-0000-0000-000000000017',
                    '68000000-0000-0000-0000-000000000016','Story 68 Authority',
                    'https://story68.example.org/evidence','APPROVED',now(),3,now());
                """);
    }

    private void assertCanonicalStory68Shape() {
        assertThat(number("SELECT count(*) FROM information_schema.tables WHERE table_schema="
                + "'public' AND table_name IN ('expert_consultation_requests',"
                + "'consultation_context_shares','consultation_context_citations')"))
                .isEqualTo(3);
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE conrelid="
                + "'public.consultation_context_shares'::regclass AND confrelid IN ("
                + "'public.expert_consultation_requests'::regclass,"
                + "'public.triage_sessions'::regclass,"
                + "'public.mother_journeys'::regclass,"
                + "'public.professional_profiles'::regclass,"
                + "'public.data_permissions'::regclass)"))
                .isEqualTo(5);
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE conrelid="
                + "'public.consultation_context_citations'::regclass AND confrelid IN ("
                + "'public.consultation_context_shares'::regclass,"
                + "'public.knowledge_sources'::regclass)"))
                .isEqualTo(2);
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE conrelid="
                + "'public.expert_consultation_requests'::regclass "
                + "AND conname='expert_consultation_requests_direct_conversation_archive_fk' "
                + "AND confrelid='public.archived_realtime_records'::regclass "
                + "AND convalidated"))
                .isOne();
        assertThat(number("SELECT count(*) FROM information_schema.tables WHERE table_schema="
                + "'public' AND table_name IN ('consultation_requests','consent_grants',"
                + "'intake_sessions','expert_profiles','evidence_sources')"))
                .isZero();
        assertThat(number("SELECT count(*) FROM information_schema.tables WHERE table_schema="
                + "'carebridge_migration_bridge' AND table_name IN ("
                + "'story68_history_state','story68_request_bridge',"
                + "'story68_context_share_bridge','story68_context_citation_bridge',"
                + "'story68_notification_reference_bridge',"
                + "'story68_audit_reference_bridge',"
                + "'story68_shadow_parent_registry')"))
                .isZero();
        long remainingBridgeObjects = number("""
                SELECT (SELECT count(*) FROM pg_class relation
                        JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                        WHERE namespace.nspname='carebridge_migration_bridge')
                     + (SELECT count(*) FROM pg_proc routine
                        JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace
                        WHERE namespace.nspname='carebridge_migration_bridge')
                     + (SELECT count(*) FROM pg_type type
                        JOIN pg_namespace namespace ON namespace.oid=type.typnamespace
                        WHERE namespace.nspname='carebridge_migration_bridge')
                """);
        if (remainingBridgeObjects == 0) {
            assertThat(number("SELECT count(*) FROM pg_namespace WHERE nspname="
                    + "'carebridge_migration_bridge'"))
                    .isZero();
        }
    }

    private Path copyNamedMigrations(Path destination, Set<String> names) throws IOException {
        return copyMigrations(destination, null, names, false);
    }

    private Path copyMigrationsThrough(
            Path destination, MigrationVersion target, Set<String> excludedNames)
            throws IOException {
        return copyMigrations(destination, target, excludedNames, true);
    }

    private Path copyMigrations(
            Path destination,
            MigrationVersion target,
            Set<String> names,
            boolean namesAreExclusions)
            throws IOException {
        Path source = migrationRoot();
        Set<String> selected = new HashSet<>(names);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                String name = path.getFileName().toString();
                MigrationVersion version = migrationVersion(name);
                boolean named = selected.contains(name);
                boolean include = namesAreExclusions
                        ? version != null && version.compareTo(target) <= 0 && !named
                        : named;
                if (!include) {
                    continue;
                }
                Path output = destination.resolve(source.relativize(path));
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
        int separator = fileName.indexOf("__");
        if (separator < 2) {
            return null;
        }
        return MigrationVersion.fromVersion(
                fileName.substring(1, separator).replace('_', '.'));
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

    private void migrate(
            Path location,
            MigrationVersion target,
            boolean outOfOrder,
            boolean validateOnMigrate) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:" + location.toAbsolutePath().toString().replace('\\', '/'))
                .target(target)
                .outOfOrder(outOfOrder)
                .validateOnMigrate(validateOnMigrate)
                .load()
                .migrate();
    }

    private long number(String sql) {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
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
