package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Contract test for wave-13 prerequisites 1-3 (V3 §3.12, migration V20260807130000).
 *
 * <p>These assertions guard a schema that nothing reads yet, which is exactly why they are
 * worth writing: the columns are inert until the growth merge is implemented, so a later
 * migration could quietly drop or redefine one and no runtime test would notice.
 *
 * <p>The suite is equally a statement about what has <em>not</em> happened — growth data must
 * still live in growth_measurements, untouched, until prerequisites 4 and 5 are settled.
 */
class Wave13PrerequisiteEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void observationsCanGroupTheReadingsOfOneMeasuringSession() {
        UUID subject = anyCareSubject();
        UUID group = UUID.randomUUID();

        insertObservation(subject, "WEIGHT", group);
        insertObservation(subject, "HEIGHT", group);
        insertObservation(subject, "HEAD_CIRCUMFERENCE", group);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM health_observations WHERE measurement_group_id = ?",
                Long.class, group))
                .isEqualTo(3L);
    }

    @Test
    void groupingIsOptionalSoExistingObservationsStayValid() {
        UUID subject = anyCareSubject();
        insertObservation(subject, "WEIGHT", null);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE care_subject_id = ? AND measurement_group_id IS NULL
                """, Long.class, subject))
                .isPositive();
    }

    @Test
    void observationsCarryASoftDeleteMarkerMatchingGrowthMeasurements() {
        UUID subject = anyCareSubject();
        UUID group = UUID.randomUUID();
        insertObservation(subject, "WEIGHT", group);

        jdbcTemplate.update(
                "UPDATE health_observations SET deleted_at = now() WHERE measurement_group_id = ?",
                group);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ? AND deleted_at IS NOT NULL
                """, Long.class, group))
                .isEqualTo(1L);
    }

    @Test
    void metricDefinitionsAcceptBabySubjects() {
        jdbcTemplate.update("""
                INSERT INTO health_metric_definitions (
                    metric_definition_id, metric_code, version, display_name,
                    observation_shape, subject_type, canonical_unit)
                VALUES (gen_random_uuid(), 'BABY_WEIGHT_PROBE', 1, 'Baby weight probe',
                        'POINT', 'BABY', 'kg')
                """);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_metric_definitions
                 WHERE metric_code = 'BABY_WEIGHT_PROBE' AND subject_type = 'BABY'
                """, Long.class))
                .isEqualTo(1L);
    }

    @Test
    void metricDefinitionsStillRejectSubjectsWaveThirteenDoesNotNeed() {
        // The constraint was widened for BABY only. DEPENDENT staying out is the point:
        // it proves the widening was scoped rather than removed.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO health_metric_definitions (
                    metric_definition_id, metric_code, version, display_name,
                    observation_shape, subject_type, canonical_unit)
                VALUES (gen_random_uuid(), 'DEPENDENT_PROBE', 1, 'Dependent probe',
                        'POINT', 'DEPENDENT', 'kg')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theBackfillIdentityMechanismIsInPlace() {
        // V3 §3.12 makes the growth backfill idempotent through (legacy_source, legacy_id)
        // with legacy_id = '<growth_measurement_id>:WEIGHT' and siblings. Without the unique
        // key a rerun would duplicate every observation.
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_constraint
                 WHERE conrelid = 'public.health_observations'::regclass AND contype = 'u'
                   AND pg_get_constraintdef(oid) = 'UNIQUE (legacy_source, legacy_id)'
                """, Long.class))
                .isEqualTo(1L);

        UUID subject = anyCareSubject();
        UUID growthId = UUID.randomUUID();
        insertLegacyObservation(subject, growthId + ":WEIGHT");

        assertThatThrownBy(() -> insertLegacyObservation(subject, growthId + ":WEIGHT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void growthMeasurementsIsUntouchedBecauseTheMergeHasNotHappened() {
        // Prerequisites only. If this fails, someone ran the merge without settling
        // V3 §3.12 conditions 4 and 5.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.growth_measurements') IS NOT NULL", Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'growth_measurements'
                   AND column_name = 'deleted_at'
                """, Long.class))
                .isEqualTo(1L);
    }

    private UUID anyCareSubject() {
        UUID subjectId = UUID.randomUUID();
        UUID userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users ORDER BY created_at LIMIT 1", UUID.class);
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'BABY', 'Wave13 probe', 'ACTIVE', now(), now()
                  FROM users u WHERE u.user_id = ?
                """, subjectId, userId);
        return subjectId;
    }

    private void insertObservation(UUID careSubjectId, String type, UUID group) {
        jdbcTemplate.update("""
                INSERT INTO health_observations (
                    health_observation_id, care_subject_id, observation_type, subject_type,
                    value_numeric, unit, observed_at, measurement_group_id)
                VALUES (gen_random_uuid(), ?, ?, 'BABY', 1, 'kg', now(), ?)
                """, careSubjectId, type, group);
    }

    private void insertLegacyObservation(UUID careSubjectId, String legacyId) {
        jdbcTemplate.update("""
                INSERT INTO health_observations (
                    health_observation_id, care_subject_id, observation_type, subject_type,
                    value_numeric, unit, observed_at, legacy_source, legacy_id)
                VALUES (gen_random_uuid(), ?, 'WEIGHT', 'BABY', 1, 'kg', now(),
                        'growth_measurements', ?)
                """, careSubjectId, legacyId);
    }
}
