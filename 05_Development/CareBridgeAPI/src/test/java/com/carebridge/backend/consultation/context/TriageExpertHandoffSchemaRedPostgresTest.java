package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Story 6.8 PostgreSQL migration and immutable-context contract tests.
 *
 * <p>The suite stays independent of planned entities and repositories. It verifies fresh Flyway
 * history, catalog structure, authoritative snapshot integrity, exact-once constraints, approved
 * citation metadata, and append-only behavior directly against PostgreSQL.
 */
class TriageExpertHandoffSchemaRedPostgresTest extends AbstractPostgresIntegrationTest {

    private static final String PRE_STORY_V1_CANONICAL_SHA256 =
            "EF0D1B28017BF32681924DED4AAF92D75427B5E5B8377B4A14F685A72CD62054";

    static {
        verifyCanonicalV1BeforeSpringFlywayStarts();
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void migrationCreatesBothAppendOnlyContextTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'consultation_context_shares',
                    'consultation_context_citations')
                ORDER BY table_name
                """,
                String.class);

        assertThat(tables).containsExactly(
                "consultation_context_citations",
                "consultation_context_shares");
    }

    @Test
    void contextShareSchemaContainsOnlyTheApprovedSnapshotAndIntegrityColumns() {
        List<String> columns = columnsFor("consultation_context_shares");

        assertThat(columns).contains(
                "context_share_id",
                "consultation_request_id",
                "owner_user_id",
                "intake_session_id",
                "expert_profile_id",
                "consent_grant_id",
                "idempotency_key",
                "journey_id",
                "origin_dashboard",
                "origin_reference_id",
                "triage_stage",
                "risk_level",
                "intake_status",
                "risk_summary",
                "share_policy_version",
                "created_at");
        assertThat(columns).doesNotContain(
                "symptoms",
                "raw_ai_response",
                "normalized_symptoms",
                "red_flags",
                "claims",
                "evidence",
                "notes",
                "recommended_action",
                "continuation_token",
                "route");
    }

    @Test
    void schemaDefinesCompositeLinkageAndExactOnceCardinalityConstraints() {
        List<String> constraints = constraintNamesFor("consultation_context_shares");

        assertThat(constraints).contains(
                "uq_context_owner_key",
                "uq_context_intake_expert",
                "fk_context_request_integrity",
                "fk_context_intake_snapshot",
                "fk_context_journey_owner",
                "fk_context_expert",
                "fk_context_consent_integrity",
                "chk_context_yellow",
                "chk_context_completed",
                "chk_context_origin",
                "chk_context_stage",
                "chk_context_summary",
                "chk_context_policy");

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                    'uq_consultation_requests_integrity',
                    'uq_consent_grants_integrity',
                    'uq_intake_handoff_integrity')
                ORDER BY indexname
                """,
                String.class);
        assertThat(indexes).containsExactlyInAnyOrder(
                "uq_consultation_requests_integrity",
                "uq_consent_grants_integrity",
                "uq_intake_handoff_integrity");
    }

    @Test
    void citationSchemaEnforcesApprovedReviewedMetadataAndUniqueSnapshotSources() {
        List<String> columns = columnsFor("consultation_context_citations");
        assertThat(columns).contains(
                "citation_snapshot_id",
                "context_share_id",
                "evidence_source_id",
                "organization",
                "source_url",
                "source_status_at_share",
                "reviewed_at",
                "ordinal",
                "created_at");
        assertThat(columns).doesNotContain(
                "title",
                "excerpt",
                "matched_symptoms",
                "matched_rules",
                "source_version",
                "claims",
                "evidence");

        assertThat(constraintNamesFor("consultation_context_citations")).contains(
                "fk_context_citation_share",
                "fk_context_citation_source",
                "uq_context_citation_source",
                "chk_context_citation_approved",
                "chk_context_citation_https",
                "chk_context_citation_ordinal");
    }

    @Test
    void bothContextTablesRejectUpdateAndDeleteThroughDatabaseTriggers() {
        List<String> triggerEvents = jdbcTemplate.queryForList(
                """
                SELECT event_object_table || ':' || event_manipulation
                FROM information_schema.triggers
                WHERE trigger_schema = 'public'
                  AND event_object_table IN (
                    'consultation_context_shares',
                    'consultation_context_citations')
                  AND event_manipulation IN ('UPDATE', 'DELETE')
                ORDER BY event_object_table, event_manipulation
                """,
                String.class);

        assertThat(triggerEvents).containsExactlyInAnyOrder(
                "consultation_context_shares:UPDATE",
                "consultation_context_shares:DELETE",
                "consultation_context_citations:UPDATE",
                "consultation_context_citations:DELETE");
    }

    @Test
    void freshHistoryAppliesStory68ExactlyOnceAfterItsStory67Prerequisite() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                """
                SELECT version, installed_rank, success
                FROM flyway_schema_history
                WHERE version IN ('20260722210000', '20260723090000')
                ORDER BY installed_rank
                """);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(row -> row.get("version").toString())
                .containsExactly("20260722210000", "20260723090000");
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
        assertThat(((Number) history.get(0).get("installed_rank")).intValue())
                .isLessThan(((Number) history.get(1).get("installed_rank")).intValue());

        Integer story68Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '20260723090000'",
                Integer.class);
        assertThat(story68Count).isEqualTo(1);
    }

    @Test
    void externalManifestPinsUnchangedV1AndPairsBothTablesToTheForwardMigration()
            throws IOException {
        String v1 = resourceText("db/migration/V1__init_schema.sql");
        String manifest = resourceText("db/schema/story-6-8-forward-schema-ownership.md");
        String story68 = resourceText(
                "db/migration/V20260723090000__create_consented_triage_expert_handoffs.sql");

        assertThat(canonicalSha256(v1)).isEqualTo(PRE_STORY_V1_CANONICAL_SHA256);
        assertThat(manifest).contains(
                PRE_STORY_V1_CANONICAL_SHA256,
                "consultation_context_shares",
                "consultation_context_citations",
                "V20260723090000__create_consented_triage_expert_handoffs.sql",
                "Prerequisites created after V1",
                "remain byte-for-byte unchanged");

        String executableV1 = v1.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + '\n' + right);
        assertThat(executableV1).doesNotContain(
                "CREATE TABLE consultation_context_shares",
                "CREATE TABLE consultation_context_citations",
                "CREATE UNIQUE INDEX uq_consultation_requests_integrity",
                "reject_consultation_context_mutation");

        assertThat(countExecutableOccurrencesAcrossMigrations(
                        "CREATE TABLE consultation_context_shares"))
                .isEqualTo(1);
        assertThat(countExecutableOccurrencesAcrossMigrations(
                        "CREATE TABLE consultation_context_citations"))
                .isEqualTo(1);
        assertThat(story68).contains(
                "CREATE TABLE consultation_context_shares",
                "CREATE TABLE consultation_context_citations");
    }

    @Test
    void preStoryDatabaseValidatesExistingChecksumsThenAppliesOnlyStory68() throws SQLException {
        try (PostgreSQLContainer preStory = new PostgreSQLContainer("postgres:16-alpine")) {
            preStory.start();

            Flyway preStoryFlyway = flywayFor(preStory, true);
            preStoryFlyway.migrate();
            assertThat(migrationCount(preStory, "20260723090000")).isZero();

            Flyway checksumValidation = Flyway.configure()
                    .dataSource(
                            preStory.getJdbcUrl(),
                            preStory.getUsername(),
                            preStory.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .outOfOrder(true)
                    .ignoreMigrationPatterns("*:pending")
                    .load();
            assertThatCode(checksumValidation::validate).doesNotThrowAnyException();

            Flyway currentFlyway = flywayFor(preStory, false);
            assertThat(currentFlyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(migrationCount(preStory, "20260723090000")).isEqualTo(1);
        }
    }

    private static void verifyCanonicalV1BeforeSpringFlywayStarts() {
        try {
            String v1 = resourceText("db/migration/V1__init_schema.sql");
            if (!PRE_STORY_V1_CANONICAL_SHA256.equals(canonicalSha256(v1))) {
                throw new IllegalStateException(
                        "V1 checksum drift detected before Story 6.8 Flyway verification");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Canonical V1 resource is unavailable", exception);
        }
    }

    private static String canonicalSha256(String source) {
        String canonical = source.replace("\r\n", "\n").replace('\r', '\n');
        try {
            return HexFormat.of()
                    .withUpperCase()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    @Test
    void compositeForeignKeyCapturesEveryAuthoritativeIntakeSnapshotComponent() {
        String definition = constraintDefinition(
                "consultation_context_shares", "fk_context_intake_snapshot");

        assertThat(definition).contains(
                "intake_session_id",
                "owner_user_id",
                "journey_id",
                "origin_dashboard",
                "origin_reference_id",
                "triage_stage",
                "risk_level",
                "intake_status",
                "REFERENCES intake_sessions(id, user_id, journey_id, origin_dashboard, "
                        + "origin_reference_id, stage, risk_level, status)");
    }

    @Test
    void changedIntakeSnapshotComponentIsRejectedAtTheDatabaseBoundary() {
        Fixture fixture = seedAggregate(false, false);

        assertThatThrownBy(() -> insertContext(
                        fixture,
                        UUID.randomUUID(),
                        fixture.contextShareId()))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("fk_context_intake_snapshot");
    }

    @Test
    void exactOnceConstraintsCoverRequestConsentOwnerKeyIntakeExpertAndCitationSource() {
        assertThat(constraintDefinition(
                        "consultation_context_shares", "consultation_context_shares_consultation_request_id_key"))
                .contains("UNIQUE (consultation_request_id)");
        assertThat(constraintDefinition(
                        "consultation_context_shares", "consultation_context_shares_consent_grant_id_key"))
                .contains("UNIQUE (consent_grant_id)");
        assertThat(constraintDefinition("consultation_context_shares", "uq_context_owner_key"))
                .contains("UNIQUE (owner_user_id, idempotency_key)");
        assertThat(constraintDefinition("consultation_context_shares", "uq_context_intake_expert"))
                .contains("UNIQUE (owner_user_id, intake_session_id, expert_profile_id)");
        assertThat(constraintDefinition(
                        "consultation_context_citations", "uq_context_citation_source"))
                .contains("UNIQUE (context_share_id, evidence_source_id)")
                .doesNotContain("source_url");
    }

    @Test
    void citationRowsRequireApprovedReviewedHttpsMetadataAndOneRowPerAuthority() {
        Fixture fixture = seedAggregate(true, true);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO consultation_context_citations
                            (context_share_id, evidence_source_id, organization, source_url,
                             source_status_at_share, reviewed_at, ordinal)
                        VALUES (?, ?, 'WHO', 'https://different.example/source',
                                'APPROVED', now(), 1)
                        """,
                        fixture.contextShareId(),
                        fixture.evidenceSourceId()))
                .isInstanceOf(DataAccessException.class);

        UUID secondSource = seedEvidenceSource("pending-" + UUID.randomUUID() + ".example");
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO consultation_context_citations
                            (context_share_id, evidence_source_id, organization, source_url,
                             source_status_at_share, reviewed_at, ordinal)
                        VALUES (?, ?, 'Pending org', 'https://pending.example',
                                'PENDING_REVIEW', now(), 1)
                        """,
                        fixture.contextShareId(),
                        secondSource))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO consultation_context_citations
                            (context_share_id, evidence_source_id, organization, source_url,
                             source_status_at_share, reviewed_at, ordinal)
                        VALUES (?, ?, 'Pending org', 'http://pending.example',
                                'APPROVED', now(), 1)
                        """,
                        fixture.contextShareId(),
                        secondSource))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO consultation_context_citations
                            (context_share_id, evidence_source_id, organization, source_url,
                             source_status_at_share, reviewed_at, ordinal)
                        VALUES (?, ?, 'Pending org', 'https://pending.example',
                                'APPROVED', NULL, 1)
                        """,
                        fixture.contextShareId(),
                        secondSource))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void appendOnlyTriggersRejectActualUpdateAndDeleteOperations() {
        Fixture fixture = seedAggregate(true, true);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE consultation_context_shares SET risk_summary = 'Changed' "
                                + "WHERE context_share_id = ?",
                        fixture.contextShareId()))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("consultation_context_shares is append-only");
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "DELETE FROM consultation_context_citations WHERE context_share_id = ?",
                        fixture.contextShareId()))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("consultation_context_citations is append-only");
    }

    private List<String> columnsFor(String tableName) {
        return jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                ORDER BY ordinal_position
                """,
                String.class,
                tableName);
    }

    private List<String> constraintNamesFor(String tableName) {
        return jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public' AND table_name = ?
                ORDER BY constraint_name
                """,
                String.class,
                tableName);
    }

    private String constraintDefinition(String tableName, String constraintName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT pg_get_constraintdef(c.oid)
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND t.relname = ?
                  AND c.conname = ?
                """,
                String.class,
                tableName,
                constraintName);
    }

    private Fixture seedAggregate(boolean withContext, boolean withCitation) {
        UUID ownerId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        seedUser(ownerId, "Schema Mother", "MOTHER");
        seedUser(expertUserId, "Schema Expert", "EXPERT");
        jdbcTemplate.update(
                """
                INSERT INTO mother_journeys
                    (journey_id, owner_user_id, journey_type, status, created_at, updated_at)
                VALUES (?, ?, 'PREGNANCY', 'ACTIVE', now(), now())
                """,
                journeyId,
                ownerId);
        jdbcTemplate.update(
                """
                INSERT INTO intake_sessions
                    (id, user_id, symptoms, risk_level, status, created_by, stage,
                     journey_id, origin_dashboard, origin_reference_id,
                     continuation_token, continuation_expires_at, created_at, completed_at)
                VALUES (?, ?, 'synthetic fixture', 'YELLOW', 'COMPLETED', ?, 'PREGNANCY',
                        ?, 'MOTHER_JOURNEY', ?, ?, now() + interval '1 hour', now(), now())
                """,
                intakeId,
                ownerId,
                ownerId,
                journeyId,
                journeyId,
                UUID.randomUUID());
        jdbcTemplate.update(
                """
                INSERT INTO expert_profiles
                    (expert_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Maternal health', 'APPROVED', 'ACTIVE', now(), now())
                """,
                expertProfileId,
                expertUserId);
        jdbcTemplate.update(
                """
                INSERT INTO consultation_requests
                    (id, requester_user_id, expert_profile_id, client_request_id,
                     topic, description, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Triage follow-up', 'Consented triage follow-up',
                        'PENDING', now() + interval '1 day', now(), now())
                """,
                requestId,
                ownerId,
                expertProfileId,
                idempotencyKey);
        Long consentId = jdbcTemplate.queryForObject(
                """
                INSERT INTO consent_grants
                    (user_id, data_type, purpose, recipient, scope_text, policy_version,
                     evidence_key, consent_given_at, expiry_at, version, created_at, updated_at)
                VALUES (?, 'EXPERT_SHARED_DATA', 'SHARE', ?, 'YELLOW triage context',
                        'YELLOW_EXPERT_CONTEXT_V1', ?, now(), now() + interval '30 days',
                        1, now(), now())
                RETURNING id
                """,
                Long.class,
                ownerId,
                expertProfileId.toString(),
                idempotencyKey);
        UUID contextShareId = UUID.randomUUID();
        Fixture fixture = new Fixture(
                ownerId,
                expertProfileId,
                journeyId,
                intakeId,
                requestId,
                consentId,
                idempotencyKey,
                contextShareId,
                null);
        if (withContext) {
            insertContext(fixture, journeyId, contextShareId);
        }

        UUID evidenceSourceId = seedEvidenceSource("approved-" + UUID.randomUUID() + ".example");
        if (withContext && withCitation) {
            jdbcTemplate.update(
                    """
                    INSERT INTO consultation_context_citations
                        (context_share_id, evidence_source_id, organization, source_url,
                         source_status_at_share, reviewed_at, ordinal)
                    VALUES (?, ?, 'Synthetic authority', 'https://approved.example',
                            'APPROVED', now(), 0)
                    """,
                    contextShareId,
                    evidenceSourceId);
        }
        return new Fixture(
                ownerId,
                expertProfileId,
                journeyId,
                intakeId,
                requestId,
                consentId,
                idempotencyKey,
                contextShareId,
                evidenceSourceId);
    }

    private void insertContext(Fixture fixture, UUID originReferenceId, UUID contextShareId) {
        jdbcTemplate.update(
                """
                INSERT INTO consultation_context_shares
                    (context_share_id, consultation_request_id, owner_user_id,
                     intake_session_id, expert_profile_id, consent_grant_id, idempotency_key,
                     journey_id, origin_dashboard, origin_reference_id, triage_stage,
                     risk_level, intake_status, risk_summary, share_policy_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'MOTHER_JOURNEY', ?, 'PREGNANCY',
                        'YELLOW', 'COMPLETED', 'Synthetic minimum risk summary',
                        'YELLOW_EXPERT_CONTEXT_V1')
                """,
                contextShareId,
                fixture.requestId(),
                fixture.ownerId(),
                fixture.intakeId(),
                fixture.expertProfileId(),
                fixture.consentId(),
                fixture.idempotencyKey(),
                fixture.journeyId(),
                originReferenceId);
    }

    private UUID seedEvidenceSource(String domain) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO evidence_sources
                    (domain, base_url, organization, category, status, discovery_mode,
                     applicable_stages, reviewed_at, created_at, updated_at)
                VALUES (?, ?, 'Synthetic authority', 'OTHER', 'APPROVED', 'MANUAL_ADMIN_ADD',
                        'PREGNANCY', now(), now(), now())
                RETURNING id
                """,
                UUID.class,
                domain,
                "https://" + domain);
    }

    private void seedUser(UUID id, String name, String role) {
        jdbcTemplate.update(
                """
                INSERT INTO users
                    (user_id, full_name, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, true, false, now(), now())
                """,
                id,
                name,
                role);
    }

    private static Flyway flywayFor(PostgreSQLContainer container, boolean stopBeforeStory68) {
        var configuration = Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (stopBeforeStory68) {
            configuration.target(MigrationVersion.fromVersion("20260722210000"));
        }
        return configuration.load();
    }

    private static int migrationCount(PostgreSQLContainer container, String version)
            throws SQLException {
        try (var connection = DriverManager.getConnection(
                        container.getJdbcUrl(), container.getUsername(), container.getPassword());
                var statement = connection.prepareStatement(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = ?")) {
            statement.setString(1, version);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int countExecutableOccurrencesAcrossMigrations(String token)
            throws IOException {
        int occurrences = 0;
        var resolver = new PathMatchingResourcePatternResolver();
        for (var resource : resolver.getResources("classpath*:db/migration/*.sql")) {
            try (var input = resource.getInputStream()) {
                String executable = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                        .lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .reduce("", (left, right) -> left + '\n' + right);
                occurrences += countOccurrences(executable, token);
            }
        }
        return occurrences;
    }

    private static String resourceText(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int countOccurrences(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }

    private record Fixture(
            UUID ownerId,
            UUID expertProfileId,
            UUID journeyId,
            UUID intakeId,
            UUID requestId,
            Long consentId,
            UUID idempotencyKey,
            UUID contextShareId,
            UUID evidenceSourceId) {}
}
