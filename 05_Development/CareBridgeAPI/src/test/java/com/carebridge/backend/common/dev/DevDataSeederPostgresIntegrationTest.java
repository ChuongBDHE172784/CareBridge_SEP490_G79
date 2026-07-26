package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.util.List;
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

    private static final String SEEDED_MOTHER_EMAILS = """
            'mother3@carebridge.dev',
            'mother4@carebridge.dev',
            'mother5@carebridge.dev',
            'mother6@carebridge.dev'
            """;

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
    void freshFlywaySchemaSeedsCanonicalMotherSubjectsAndRemainsIdempotent() {
        assertCanonicalSeedState();
        assertLatestDevSchemaSeedState();

        Integer journeyCountBefore = seededJourneyCount();
        Integer evidenceCountBefore = seededStory65EvidenceCount();
        List<UUID> growthIdsBefore = seededGrowthMeasurementIds();
        List<Long> fixtureCountsBefore = fixtureCounts();

        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertCanonicalSeedState();
        assertLatestDevSchemaSeedState();
        assertThat(seededJourneyCount()).isEqualTo(journeyCountBefore);
        assertThat(seededStory65EvidenceCount()).isEqualTo(evidenceCountBefore);
        assertThat(seededGrowthMeasurementIds()).containsExactlyElementsOf(growthIdsBefore);
        assertThat(fixtureCounts()).isEqualTo(fixtureCountsBefore);
    }

    private void assertCanonicalSeedState() {
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
                   AND subject.person_id = owner.person_id
                   AND subject.mother_journey_id = journey.journey_id
                """.formatted(SEEDED_MOTHER_EMAILS), Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM mother_journey_events evidence
                  JOIN mother_journeys journey ON journey.journey_id = evidence.mother_journey_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN ('mother5@carebridge.dev', 'mother6@carebridge.dev')
                   AND evidence.legacy_source = 'PREGNANCY_OUTCOME'
                   AND evidence.owner_user_id = journey.owner_user_id
                   AND evidence.journey_version = journey.version
                """, Integer.class)).isEqualTo(2);
        assertThat(seededGrowthMeasurementIds()).containsExactlyElementsOf(SEEDED_GROWTH_IDS);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM growth_measurements measurement
                  JOIN care_subjects baby ON baby.care_subject_id = measurement.baby_id
                                         AND baby.subject_type = 'BABY'
                  JOIN care_subjects subject ON subject.care_subject_id = measurement.care_subject_id
                  JOIN users owner ON owner.user_id = baby.owner_user_id
                 WHERE measurement.note LIKE '[DEV][MF-03]%'
                   AND measurement.care_subject_id = measurement.baby_id
                   AND subject.subject_type = 'BABY'
                   AND subject.owner_user_id = baby.owner_user_id
                   AND owner.email = 'mother4@carebridge.dev'
                """, Integer.class)).isEqualTo(5);
    }

    private void assertLatestDevSchemaSeedState() {
        assertThat(count("SELECT count(*) FROM users WHERE email LIKE '%@carebridge.dev'"))
                .isGreaterThanOrEqualTo(14L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_credentials ec
                  JOIN professional_profiles pp
                    ON pp.professional_profile_id = ec.professional_profile_id
                  JOIN users u ON u.user_id = pp.user_id
                 WHERE u.email IN ('expert2@carebridge.dev', 'expert3@carebridge.dev')
                """)).isEqualTo(2L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_availability ea
                  JOIN professional_profiles pp
                    ON pp.professional_profile_id = ea.professional_profile_id
                  JOIN users u ON u.user_id = pp.user_id
                 WHERE u.email IN ('expert2@carebridge.dev', 'expert3@carebridge.dev')
                """)).isEqualTo(2L);
    }

    private List<Long> fixtureCounts() {
        return List.of(
                count("SELECT count(*) FROM users"),
                count("SELECT count(*) FROM persons"),
                count("SELECT count(*) FROM care_subjects"),
                count("SELECT count(*) FROM mother_journeys"),
                count("SELECT count(*) FROM mother_journey_events"),
                count("SELECT count(*) FROM growth_measurements"),
                count("SELECT count(*) FROM care_logs"),
                count("SELECT count(*) FROM professional_profiles"),
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

    private Integer seededStory65EvidenceCount() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM mother_journey_events evidence
                  JOIN mother_journeys journey ON journey.journey_id = evidence.mother_journey_id
                  JOIN users owner ON owner.user_id = journey.owner_user_id
                 WHERE owner.email IN ('mother5@carebridge.dev', 'mother6@carebridge.dev')
                   AND evidence.legacy_source = 'PREGNANCY_OUTCOME'
                """, Integer.class);
    }

    private List<UUID> seededGrowthMeasurementIds() {
        return jdbcTemplate.queryForList("""
                SELECT growth_measurement_id
                  FROM growth_measurements
                 WHERE note LIKE '[DEV][MF-03]%'
                ORDER BY growth_measurement_id
                """, UUID.class);
    }
}
