package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the wave-13 backfill (V20260807140000) against rows that actually exist.
 *
 * <p>The migration runs at startup against an empty growth_measurements, so its own gates
 * pass vacuously — proving nothing about the transformation. These tests seed real source
 * rows and replay the backfill statement, so the mapping in
 * {@code 08_References/Wave13_Growth_To_Observations_Mapping.md} is checked rather than
 * assumed.
 *
 * <p>The statement is read out of the migration file instead of being retyped, so the test
 * cannot drift away from what production will run.
 */
class Wave13GrowthBackfillEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration",
            "V20260807140000__expand_growth_measurements_into_health_observations.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void oneGrowthRowBecomesOneObservationPerRecordedMeasurement() throws IOException {
        UUID baby = seedBaby();
        UUID full = seedGrowth(baby, LocalDate.of(2026, 6, 6),
                new BigDecimal("3.40"), new BigDecimal("50.00"), new BigDecimal("34.00"),
                "HOME", "Khám định kỳ");

        runBackfill();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT observation_type, value_numeric, unit, subject_type, source_type,
                       measurement_group_id, legacy_source, legacy_id,
                       context_jsonb->>'measurementSetting' AS setting,
                       context_jsonb->>'note' AS note,
                       (observed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS local_date
                  FROM health_observations
                 WHERE measurement_group_id = ? ORDER BY observation_type
                """, full);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(r -> r.get("observation_type"))
                .containsExactly("BABY_HEAD_CIRCUMFERENCE", "BABY_HEIGHT", "BABY_WEIGHT");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("subject_type")).isEqualTo("BABY");
            // growth's own vocabulary answers "measured where", which is not what
            // health_observations.source_type means; it is kept in context instead.
            assertThat(row.get("source_type")).isEqualTo("MANUAL");
            assertThat(row.get("setting")).isEqualTo("HOME");
            assertThat(row.get("note")).isEqualTo("Khám định kỳ");
            assertThat(row.get("legacy_source")).isEqualTo("growth_measurements");
            assertThat(row.get("local_date").toString()).isEqualTo("2026-06-06");
        });
        assertThat(rows).extracting(r -> ((BigDecimal) r.get("value_numeric")).stripTrailingZeros())
                .containsExactly(
                        new BigDecimal("34.00").stripTrailingZeros(),
                        new BigDecimal("50.00").stripTrailingZeros(),
                        new BigDecimal("3.40").stripTrailingZeros());
        assertThat(rows).extracting(r -> r.get("legacy_id"))
                .containsExactly(
                        full + ":BABY_HEAD_CIRCUMFERENCE",
                        full + ":BABY_HEIGHT",
                        full + ":BABY_WEIGHT");
    }

    @Test
    void aMeasurementThatWasNotRecordedProducesNoObservation() throws IOException {
        UUID baby = seedBaby();
        UUID weightOnly = seedGrowth(baby, LocalDate.of(2026, 7, 4),
                new BigDecimal("3.90"), null, null, "CLINIC", null);

        runBackfill();

        // A session that weighed the baby must not invent a height of zero.
        assertThat(jdbcTemplate.queryForList("""
                SELECT observation_type FROM health_observations WHERE measurement_group_id = ?
                """, String.class, weightOnly))
                .containsExactly("BABY_WEIGHT");
        // jsonb_strip_nulls keeps an absent note out of the payload entirely.
        // jsonb_exists rather than the ? operator, which JDBC would read as a placeholder.
        assertThat(jdbcTemplate.queryForObject("""
                SELECT jsonb_exists(context_jsonb, 'note') FROM health_observations
                 WHERE measurement_group_id = ?
                """, Boolean.class, weightOnly))
                .isFalse();
    }

    @Test
    void rerunningTheBackfillInsertsNothingNew() throws IOException {
        UUID baby = seedBaby();
        UUID session = seedGrowth(baby, LocalDate.of(2026, 8, 1),
                new BigDecimal("4.40"), new BigDecimal("58.00"), null, "HOME_SCALE", "Tại nhà");

        runBackfill();
        long totalAfterFirst = countMigrated();
        runBackfill();
        runBackfill();

        // The contract step reruns this immediately before dropping growth_measurements, so
        // idempotency is load-bearing, not a nicety. The totals are class-wide because this
        // suite shares one database; the per-session count is what pins the shape.
        assertThat(countMigrated()).isEqualTo(totalAfterFirst);
        assertThat(countMigratedForSession(session)).isEqualTo(2L);
    }

    @Test
    void softDeleteStateTravelsWithTheMeasurement() throws IOException {
        UUID baby = seedBaby();
        UUID deleted = seedGrowth(baby, LocalDate.of(2026, 5, 1),
                new BigDecimal("3.10"), null, null, "HOME", null);
        jdbcTemplate.update(
                "UPDATE growth_measurements SET deleted_at = now() WHERE growth_measurement_id = ?",
                deleted);

        runBackfill();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT deleted_at IS NOT NULL FROM health_observations
                 WHERE measurement_group_id = ?
                """, Boolean.class, deleted))
                .isTrue();
    }

    @Test
    void babyMetricsAreDefinedWithCodesOfTheirOwn() {
        // Reusing 'WEIGHT' would collide with the MOTHER definition on (metric_code, version)
        // and would let a query that forgets subject_type mix a newborn with an adult.
        assertThat(jdbcTemplate.queryForList("""
                SELECT metric_code FROM health_metric_definitions
                 WHERE subject_type = 'BABY' ORDER BY metric_code
                """, String.class))
                .containsExactly("BABY_HEAD_CIRCUMFERENCE", "BABY_HEIGHT", "BABY_WEIGHT");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT subject_type FROM health_metric_definitions WHERE metric_code = 'WEIGHT'
                """, String.class))
                .isEqualTo("MOTHER");
    }

    private long countMigrated() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM health_observations WHERE legacy_source = 'growth_measurements'",
                Long.class);
    }

    private long countMigratedForSession(UUID growthMeasurementId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE legacy_source = 'growth_measurements' AND measurement_group_id = ?
                """, Long.class, growthMeasurementId);
    }

    /** Replays the migration's own INSERT so the test cannot drift from production. */
    private void runBackfill() throws IOException {
        String sql = Files.readString(MIGRATION);
        int start = sql.indexOf("INSERT INTO public.health_observations (");
        assertThat(start).as("backfill statement present in migration").isNotNegative();
        int end = sql.indexOf("\n\n-- ---", start);
        assertThat(end).as("backfill statement terminated").isGreaterThan(start);
        jdbcTemplate.execute(sql.substring(start, end).trim());
    }

    private UUID seedBaby() {
        UUID subjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'BABY', 'Wave13 backfill', 'ACTIVE', now(), now()
                  FROM users u ORDER BY u.created_at LIMIT 1
                """, subjectId);
        return subjectId;
    }

    private UUID seedGrowth(UUID careSubjectId, LocalDate measuredOn, BigDecimal weight,
            BigDecimal height, BigDecimal headCircumference, String sourceType, String note) {
        UUID growthId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO growth_measurements (
                    growth_measurement_id, baby_id, care_subject_id, measured_date,
                    weight_kg, height_cm, head_circumference_cm, source_type, note,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """, growthId, careSubjectId, careSubjectId, measuredOn,
                weight, height, headCircumference, sourceType, note);
        return growthId;
    }
}
