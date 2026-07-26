package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "carebridge.dev-seed.enabled=true",
        "carebridge.dev-seed.password=Synthetic-Only-Strong-Passphrase-6-10",
        "spring.main.allow-bean-definition-overriding=true"
})
class DevDataSeederPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

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

    @Test
    void freshFlywaySchemaSeedsCanonicalMotherSubjectsAndRemainsIdempotent() {
        assertCanonicalSeedState();

        Integer journeyCountBefore = seededJourneyCount();
        Integer evidenceCountBefore = seededStory65EvidenceCount();
        List<UUID> growthIdsBefore = seededGrowthMeasurementIds();

        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertCanonicalSeedState();
        assertThat(seededJourneyCount()).isEqualTo(journeyCountBefore);
        assertThat(seededStory65EvidenceCount()).isEqualTo(evidenceCountBefore);
        assertThat(seededGrowthMeasurementIds()).containsExactlyElementsOf(growthIdsBefore);
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
