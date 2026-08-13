package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.out-of-order=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "carebridge.mail.from-address=noreply@carebridge.test",
        "carebridge.mail.from-name=CareBridge",
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret",
        "carebridge.dev-seed.enabled=true",
        "carebridge.dev-seed.password=Synthetic-Only-Strong-Passphrase-6-10",
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevDataSeederPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.1-alpine");

    static {
        // This class brings its own container, so it does not inherit the role provisioning
        // in AbstractPostgresIntegrationTest. Without these NOLOGIN owners the checklist
        // migrations refuse to run (CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED) and the context
        // never boots.
        POSTGRES.start();
        try {
            com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture.provision(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not provision checklist database roles on the dev-seed container", exception);
        }
    }

    private static final String SEEDED_MOTHER_EMAILS = """
            'mother3@carebridge.dev',
            'mother4@carebridge.dev',
            'mother5@carebridge.dev',
            'mother6@carebridge.dev'
            """;

    private static final List<String> EXPECTED_SEED_EMAILS = List.of(
            "admin@carebridge.dev", "moderator@carebridge.dev", "content@carebridge.dev",
            "expert@carebridge.dev", "mother@carebridge.dev", "family@carebridge.dev",
            "mother2@carebridge.dev", "mother3@carebridge.dev", "mother4@carebridge.dev",
            "mother5@carebridge.dev", "mother6@carebridge.dev", "family2@carebridge.dev",
            "family3@carebridge.dev", "family4@carebridge.dev", "family5@carebridge.dev",
            "family6@carebridge.dev", "expert2@carebridge.dev", "expert3@carebridge.dev",
            "expert4@carebridge.dev", "expert5@carebridge.dev", "expert6@carebridge.dev");

    private static final Map<String, String> EXPECTED_SEED_NAMES = expectedSeedNames();

    private static final List<UUID> SEEDED_GROWTH_IDS = List.of(
            UUID.fromString("f0310000-0000-0000-0000-000000000001"),
            UUID.fromString("f0310000-0000-0000-0000-000000000002"),
            UUID.fromString("f0310000-0000-0000-0000-000000000003"),
            UUID.fromString("f0310000-0000-0000-0000-000000000004"),
            UUID.fromString("f0310000-0000-0000-0000-000000000005"));

    @Autowired
    private DevDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Test
    void freshFlywaySchemaUpgradesLegacyStandaloneFixturesAndRemainsIdempotent() {
        assertCanonicalSeedState(2);
        assertLatestDevSchemaSeedState();

        insertObsoleteUnreferencedSeedAccount();
        assertThat(count("SELECT count(*) FROM users WHERE email = 'mebau@carebridge.dev'"))
                .isEqualTo(1L);
        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertThat(count("SELECT count(*) FROM users WHERE email = 'mebau@carebridge.dev'"))
                .isZero();
        assertLatestDevSchemaSeedState();

        List<UUID> standaloneBabyIdsBefore = seededStandaloneBabyIds();
        renameStandaloneBabyFixturesToLegacyNames();
        replaceStandaloneEvidenceWithLegacySubmissionIds();
        assertThat(seededStandaloneBabyIds()).isEmpty();
        assertThat(seededLegacyStandaloneBabyCount()).isEqualTo(2L);
        assertThat(seededEvidenceSubmissionCount("standalone-baby-")).isZero();
        assertThat(seededEvidenceSubmissionCount("story65-")).isEqualTo(2L);
        assertAuditEventsImmutableTriggerEnabled();

        Integer journeyCountBefore = seededJourneyCount();
        Integer evidenceCountBefore = seededPregnancyOutcomeEvidenceCount();
        List<UUID> growthIdsBefore = seededGrowthMeasurementIds();
        List<Long> fixtureCountsBefore = fixtureCounts();

        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertCanonicalSeedState(evidenceCountBefore);
        assertLatestDevSchemaSeedState();
        assertThat(seededJourneyCount()).isEqualTo(journeyCountBefore);
        assertThat(seededPregnancyOutcomeEvidenceCount()).isEqualTo(evidenceCountBefore);
        assertThat(seededGrowthMeasurementIds()).containsExactlyElementsOf(growthIdsBefore);
        assertThat(fixtureCounts()).isEqualTo(fixtureCountsBefore);
        assertThat(seededStandaloneBabyIds()).containsExactlyElementsOf(standaloneBabyIdsBefore);
        assertThat(seededLegacyStandaloneBabyCount()).isZero();
        assertThat(seededEvidenceSubmissionCount("standalone-baby-")).isZero();
        assertThat(seededEvidenceSubmissionCount("story65-")).isEqualTo(2L);
        assertAuditEventsImmutableTriggerEnabled();
    }

    private void insertObsoleteUnreferencedSeedAccount() {
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, person_id, email, password_hash, full_name, display_name,
                     role, enabled, locked, account_status, email_verified, phone_verified,
                     settings_jsonb, social_identities, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'mebau@carebridge.dev', 'retired-seed-hash',
                        'Mẹ Bầu Mới', 'Mẹ Bầu Mới', 'MOTHER', TRUE, FALSE, 'ACTIVE',
                        TRUE, FALSE, '{}'::jsonb, '[]'::jsonb, 'PENDING', 'ACTIVE', NOW(), NOW())
                """, UUID.randomUUID(), UUID.randomUUID());
    }

    private void assertCanonicalSeedState(int expectedEvidenceCount) {
        assertThat(seededJourneyCount()).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM mother_journeys journey
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                  JOIN care_subjects subject ON subject.care_subject_id = journey.care_subject_id
                 WHERE owner.email IN (%s)
                   AND owner.role = 'MOTHER'
                   AND subject.subject_type = 'MOTHER'
                   AND subject.owner_user_id = journey.owner_user_id
                   AND subject.person_id = owner.user_id
                   AND subject.mother_journey_id = journey.journey_id
                """.formatted(SEEDED_MOTHER_EMAILS), Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM audit_events evidence
                  JOIN mother_journeys journey ON journey.journey_id = evidence.subject_reference_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN ('mother5@carebridge.dev', 'mother6@carebridge.dev')
                   AND evidence.event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                   AND evidence.actor_user_id = journey.owner_user_id
                   AND (evidence.payload->>'journeyVersion')::bigint = journey.version
                """, Integer.class)).isEqualTo(expectedEvidenceCount);
        assertThat(seededGrowthMeasurementIds()).containsExactlyElementsOf(SEEDED_GROWTH_IDS);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM care_subjects baby
                  JOIN users owner ON owner.user_id = baby.owner_user_id
                 WHERE baby.subject_type = 'BABY'
                   AND owner.email LIKE '%@carebridge.dev'
                   AND baby.mother_journey_id IS NOT NULL
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                -- Sessions, not rows: one seeded measuring session is three observations
                -- sharing a measurement_group_id, so the count is over groups to keep the
                -- assertion saying what it always said.
                SELECT count(DISTINCT measurement.measurement_group_id)
                  FROM health_observations measurement
                  JOIN care_subjects baby ON baby.care_subject_id = measurement.care_subject_id
                                         AND baby.subject_type = 'BABY'
                  JOIN users owner ON owner.user_id = baby.owner_user_id
                 WHERE measurement.legacy_source = 'growth_measurements'
                   AND measurement.context_jsonb->>'note' LIKE '[DEV][MF-03]%'
                   AND measurement.subject_type = 'BABY'
                   AND owner.email = 'mother4@carebridge.dev'
                """, Integer.class)).isEqualTo(5);
    }

    private void assertLatestDevSchemaSeedState() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT email
                  FROM users
                 WHERE email LIKE '%@carebridge.dev'
                 ORDER BY email
                """, String.class)).containsExactlyInAnyOrderElementsOf(EXPECTED_SEED_EMAILS);
        Map<String, String> actualNames = jdbcTemplate.query("""
                SELECT email, full_name
                  FROM users
                 WHERE email LIKE '%@carebridge.dev'
                """, resultSet -> {
            Map<String, String> names = new LinkedHashMap<>();
            while (resultSet.next()) {
                names.put(resultSet.getString("email"), resultSet.getString("full_name"));
            }
            return names;
        });
        assertThat(actualNames).containsExactlyInAnyOrderEntriesOf(EXPECTED_SEED_NAMES);
        assertThat(count("""
                SELECT count(*)
                  FROM users
                 WHERE email LIKE '%@carebridge.dev'
                   AND phone IS NOT NULL
                   AND date_of_birth IS NOT NULL
                   AND area IS NOT NULL
                   AND enabled = TRUE
                   AND account_status = 'ACTIVE'
                   AND email_verified = TRUE
                   AND phone_verified = TRUE
                """)).isEqualTo(21L);
        assertThat(count("SELECT count(DISTINCT phone) FROM users WHERE email LIKE '%@carebridge.dev'"))
                .isEqualTo(21L);
        assertThat(count("""
                SELECT count(*)
                  FROM care_group_members member
                  JOIN care_groups group_row ON group_row.care_group_id = member.care_group_id
                  JOIN users actor ON actor.user_id = member.user_id
                  JOIN users owner ON owner.user_id = group_row.owner_user_id
                 WHERE member.invitation_status = 'ACCEPTED'
                   AND ((owner.email = 'mother3@carebridge.dev'
                         AND actor.email IN ('mother3@carebridge.dev', 'family2@carebridge.dev'))
                     OR (owner.email = 'mother4@carebridge.dev'
                         AND actor.email IN ('mother4@carebridge.dev', 'family3@carebridge.dev')))
                """)).isEqualTo(4L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_credentials ec
                  JOIN users u ON u.user_id = ec.user_id
                 WHERE u.email IN ('expert@carebridge.dev', 'expert2@carebridge.dev',
                                   'expert3@carebridge.dev', 'expert4@carebridge.dev',
                                   'expert5@carebridge.dev', 'expert6@carebridge.dev')
                   AND u.verification_status = 'APPROVED'
                   AND u.trust_status = 'ACTIVE'
                   AND u.specialty IS NOT NULL
                   AND u.professional_title IS NOT NULL
                   AND u.experience_years IS NOT NULL
                   AND u.workplace IS NOT NULL
                   AND u.consultation_scope IS NOT NULL
                   AND u.consultation_fee_vnd IS NOT NULL
                """)).isEqualTo(6L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_availability ea
                  JOIN users u ON u.user_id = ea.user_id
                 WHERE u.email IN ('expert@carebridge.dev', 'expert2@carebridge.dev',
                                   'expert3@carebridge.dev', 'expert4@carebridge.dev',
                                   'expert5@carebridge.dev', 'expert6@carebridge.dev')
                   AND u.verification_status = 'APPROVED'
                """)).isEqualTo(6L);
    }

    private static Map<String, String> expectedSeedNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("admin@carebridge.dev", "System Admin");
        names.put("moderator@carebridge.dev", "Moderator");
        names.put("content@carebridge.dev", "Content Admin");
        names.put("expert@carebridge.dev", "BS Đỗ Hải Long");
        names.put("mother@carebridge.dev", "Mẹ Bầu 1");
        names.put("family@carebridge.dev", "Chồng Mẹ Bầu 1");
        names.put("mother2@carebridge.dev", "Mẹ Bầu 2");
        names.put("mother3@carebridge.dev", "Mẹ bầu 3");
        names.put("mother4@carebridge.dev", "Mẹ bầu 4");
        names.put("mother5@carebridge.dev", "Mẹ bầu 5");
        names.put("mother6@carebridge.dev", "Mẹ bầu 6");
        names.put("family2@carebridge.dev", "Chồng Mẹ Bầu 2");
        names.put("family3@carebridge.dev", "Chồng Mẹ Bầu 3");
        names.put("family4@carebridge.dev", "Chồng Mẹ Bầu 4");
        names.put("family5@carebridge.dev", "Chồng Mẹ Bầu 5");
        names.put("family6@carebridge.dev", "Chồng Mẹ Bầu 6");
        names.put("expert2@carebridge.dev", "BS Trần Thị Thu Nga");
        names.put("expert3@carebridge.dev", "BS Trần Văn Hoàng");
        names.put("expert4@carebridge.dev", "BS Nguyễn Văn Minh");
        names.put("expert5@carebridge.dev", "BS Nguyễn Thị Lan");
        names.put("expert6@carebridge.dev", "BS Lê Văn Bình");
        return Map.copyOf(names);
    }

    private List<Long> fixtureCounts() {
        return List.of(
                count("SELECT count(*) FROM users"),
                count("SELECT count(*) FROM users WHERE display_name IS NOT NULL"),
                count("SELECT count(*) FROM care_subjects"),
                count("SELECT count(*) FROM mother_journeys"),
                count("SELECT count(*) FROM audit_events"),
                count("SELECT count(DISTINCT measurement_group_id) FROM health_observations WHERE legacy_source = 'growth_measurements'"),
                count("SELECT count(*) FROM care_logs"),
                count("SELECT count(*) FROM users WHERE verification_status IS NOT NULL"),
                count("SELECT count(*) FROM expert_credentials"),
                count("SELECT count(*) FROM expert_availability"),
                count("SELECT count(*) FROM community_content"),
                count("SELECT count(*) FROM content_items"));
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }

    private Integer seededJourneyCount() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM mother_journeys journey
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN (%s)
                """.formatted(SEEDED_MOTHER_EMAILS), Integer.class);
    }

    private Integer seededPregnancyOutcomeEvidenceCount() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM audit_events evidence
                  JOIN mother_journeys journey ON journey.journey_id = evidence.subject_reference_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN ('mother5@carebridge.dev', 'mother6@carebridge.dev')
                   AND evidence.event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                """, Integer.class);
    }

    private List<UUID> seededGrowthMeasurementIds() {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT measurement_group_id
                  FROM health_observations
                 WHERE legacy_source = 'growth_measurements'
                   AND context_jsonb->>'note' LIKE '[DEV][MF-03]%'
                ORDER BY measurement_group_id
                """, UUID.class);
    }

    private List<UUID> seededStandaloneBabyIds() {
        return jdbcTemplate.queryForList("""
                SELECT baby.care_subject_id
                  FROM care_subjects baby
                  JOIN users owner ON owner.user_id = baby.owner_user_id
                 WHERE owner.email = 'mother6@carebridge.dev'
                   AND baby.subject_type = 'BABY'
                   AND baby.nickname IN ('[DEV][Standalone] Baby A', '[DEV][Standalone] Baby B')
                ORDER BY baby.nickname
                """, UUID.class);
    }

    private long seededLegacyStandaloneBabyCount() {
        return count("""
                SELECT count(*)
                  FROM care_subjects baby
                  JOIN users owner ON owner.user_id = baby.owner_user_id
                 WHERE owner.email = 'mother6@carebridge.dev'
                   AND baby.subject_type = 'BABY'
                   AND baby.nickname IN ('[DEV][Story 6.5] Baby A', '[DEV][Story 6.5] Baby B')
                """);
    }

    private void renameStandaloneBabyFixturesToLegacyNames() {
        assertThat(jdbcTemplate.update("""
                UPDATE care_subjects
                   SET nickname = CASE nickname
                       WHEN '[DEV][Standalone] Baby A' THEN '[DEV][Story 6.5] Baby A'
                       WHEN '[DEV][Standalone] Baby B' THEN '[DEV][Story 6.5] Baby B'
                   END
                 WHERE owner_user_id = (
                       SELECT user_id FROM users WHERE email = 'mother6@carebridge.dev')
                   AND subject_type = 'BABY'
                   AND nickname IN ('[DEV][Standalone] Baby A', '[DEV][Standalone] Baby B')
                """)).isEqualTo(2);
    }

    private void replaceStandaloneEvidenceWithLegacySubmissionIds() {
        jdbcTemplate.execute("ALTER TABLE audit_events DISABLE TRIGGER audit_events_immutable_trg");
        try {
            replaceEvidenceSubmissionId("mother5@carebridge.dev", "mother5");
            replaceEvidenceSubmissionId("mother6@carebridge.dev", "mother6");
        } finally {
            jdbcTemplate.execute("ALTER TABLE audit_events ENABLE TRIGGER audit_events_immutable_trg");
        }
    }

    private void replaceEvidenceSubmissionId(String ownerEmail, String fixtureKey) {
        UUID currentId = evidenceSubmissionId("standalone-baby-", fixtureKey);
        UUID legacyId = evidenceSubmissionId("story65-", fixtureKey);
        UUID legacyEventId = UUID.nameUUIDFromBytes(
                ("story65-" + fixtureKey + "-evidence-row").getBytes(StandardCharsets.UTF_8));
        assertThat(jdbcTemplate.update("""
                INSERT INTO audit_events (
                    audit_event_id, actor_user_id, event_category, subject_reference_id,
                    resource_type, resource_id, occurred_at, created_at, event_origin, payload
                )
                SELECT ?, evidence.actor_user_id, evidence.event_category,
                       evidence.subject_reference_id, evidence.resource_type, evidence.resource_id,
                       evidence.occurred_at - interval '1 millisecond', now(), evidence.event_origin,
                       evidence.payload || jsonb_build_object(
                           'submissionId', CAST(? AS text),
                           'reason', '[DEV][Story 6.5] synthetic live-birth fixture',
                           'semanticHash', ?)
                  FROM audit_events evidence
                  JOIN mother_journeys journey
                    ON journey.journey_id = evidence.subject_reference_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE evidence.event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                   AND evidence.subject_reference_id = journey.journey_id
                   AND owner.email = ?
                   AND evidence.payload->>'submissionId' = ?
                """, legacyEventId, legacyId,
                "dev-story65-" + fixtureKey + "-live-birth", ownerEmail, currentId.toString()))
                .isEqualTo(1);
        assertThat(jdbcTemplate.update("""
                DELETE FROM audit_events
                 WHERE event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                   AND payload->>'submissionId' = ?
                """, currentId.toString())).isEqualTo(1);
    }

    private void assertAuditEventsImmutableTriggerEnabled() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT tgenabled
                  FROM pg_trigger
                 WHERE tgrelid = 'public.audit_events'::regclass
                   AND tgname = 'audit_events_immutable_trg'
                """, String.class)).isEqualTo("O");
    }

    private long seededEvidenceSubmissionCount(String prefix) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM audit_events evidence
                  JOIN mother_journeys journey ON journey.journey_id = evidence.subject_reference_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN ('mother5@carebridge.dev', 'mother6@carebridge.dev')
                   AND evidence.event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                   AND evidence.payload->>'submissionId' IN (?, ?)
                """, Long.class,
                evidenceSubmissionId(prefix, "mother5").toString(),
                evidenceSubmissionId(prefix, "mother6").toString());
    }

    private UUID evidenceSubmissionId(String prefix, String fixtureKey) {
        return UUID.nameUUIDFromBytes(
                (prefix + fixtureKey + "-live-birth").getBytes(StandardCharsets.UTF_8));
    }
}
