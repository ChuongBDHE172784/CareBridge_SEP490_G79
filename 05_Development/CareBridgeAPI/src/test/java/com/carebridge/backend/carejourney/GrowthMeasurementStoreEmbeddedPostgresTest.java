package com.carebridge.backend.carejourney;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementStore;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Contract test for the wave-13 cutover: growth sessions now live in health_observations.
 *
 * <p>{@code GrowthServiceTest} mocks this store, so nothing there proves the projection works.
 * These tests exercise the real thing against PostgreSQL.
 */
class GrowthMeasurementStoreEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private GrowthMeasurementStore store;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void aSessionIsStoredAsOneObservationPerRecordedMeasurement() {
        UUID baby = seedBaby();

        GrowthMeasurement saved = store.save(GrowthMeasurement.builder()
                .babyId(baby)
                .measuredDate(LocalDate.of(2026, 6, 6))
                .weightKg(new BigDecimal("3.40"))
                .heightCm(new BigDecimal("50.00"))
                .headCircumferenceCm(new BigDecimal("34.00"))
                .sourceType("HOME")
                .note("Khám định kỳ")
                .build());

        assertThat(observationTypes(saved.getGrowthMeasurementId()))
                .containsExactly("BABY_HEAD_CIRCUMFERENCE", "BABY_HEIGHT", "BABY_WEIGHT");

        GrowthMeasurement read = store.findById(saved.getGrowthMeasurementId()).orElseThrow();
        assertThat(read.getMeasuredDate()).isEqualTo(LocalDate.of(2026, 6, 6));
        assertThat(read.getWeightKg()).isEqualByComparingTo("3.40");
        assertThat(read.getHeightCm()).isEqualByComparingTo("50.00");
        assertThat(read.getHeadCircumferenceCm()).isEqualByComparingTo("34.00");
        assertThat(read.getSourceType()).isEqualTo("HOME");
        assertThat(read.getNote()).isEqualTo("Khám định kỳ");
        assertThat(read.getDeletedAt()).isNull();
    }

    @Test
    void growthRowsCarryTheLegacySourceThatKeepsThemOutOfMaternalQueries() {
        UUID baby = seedBaby();
        GrowthMeasurement saved = store.save(session(baby, LocalDate.of(2026, 7, 1),
                new BigDecimal("6.20")));

        // Every maternal query in HealthObservationRepository filters on
        // legacy_source = 'maternal_health_observations'. Writing growth rows under a
        // different source is what makes that separation hold.
        assertThat(jdbcTemplate.queryForList("""
                SELECT DISTINCT legacy_source FROM health_observations
                 WHERE measurement_group_id = ?
                """, String.class, saved.getGrowthMeasurementId()))
                .containsExactly("growth_measurements");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ? AND subject_type <> 'BABY'
                """, Long.class, saved.getGrowthMeasurementId()))
                .isZero();
    }

    @Test
    void updatingASessionKeepsItsIdentityInsteadOfCreatingAnother() {
        UUID baby = seedBaby();
        GrowthMeasurement saved = store.save(session(baby, LocalDate.of(2026, 7, 4),
                new BigDecimal("3.90")));

        saved.setWeightKg(new BigDecimal("4.10"));
        store.save(saved);

        assertThat(store.findById(saved.getGrowthMeasurementId()).orElseThrow().getWeightKg())
                .isEqualByComparingTo("4.10");
        assertThat(store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(baby))
                .extracting(GrowthMeasurement::getGrowthMeasurementId)
                .containsExactly(saved.getGrowthMeasurementId());
    }

    @Test
    void droppingOneMeasurementSoftDeletesOnlyThatReadingAndLeavesTheSessionLive() {
        UUID baby = seedBaby();
        GrowthMeasurement saved = store.save(GrowthMeasurement.builder()
                .babyId(baby)
                .measuredDate(LocalDate.of(2026, 7, 18))
                .weightKg(new BigDecimal("4.15"))
                .heightCm(new BigDecimal("56.00"))
                .build());

        saved.setHeightCm(null);
        store.save(saved);

        GrowthMeasurement read = store.findById(saved.getGrowthMeasurementId()).orElseThrow();
        assertThat(read.getWeightKg()).isEqualByComparingTo("4.15");
        assertThat(read.getHeightCm()).isNull();
        // A session keeps its identity while any measurement survives, so it must still be
        // listed — treating a partly deleted group as deleted would hide a live reading.
        assertThat(read.getDeletedAt()).isNull();
        assertThat(store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(baby))
                .extracting(GrowthMeasurement::getGrowthMeasurementId)
                .contains(saved.getGrowthMeasurementId());
        // Soft delete, not removal: the legacy_id must keep its place so a backfill rerun
        // cannot resurrect the reading.
        assertThat(jdbcTemplate.queryForObject("""
                SELECT deleted_at IS NOT NULL FROM health_observations
                 WHERE measurement_group_id = ? AND observation_type = 'BABY_HEIGHT'
                """, Boolean.class, saved.getGrowthMeasurementId()))
                .isTrue();
    }

    @Test
    void deletingASessionTakesEveryReadingWithIt() {
        UUID baby = seedBaby();
        GrowthMeasurement saved = store.save(GrowthMeasurement.builder()
                .babyId(baby)
                .measuredDate(LocalDate.of(2026, 8, 1))
                .weightKg(new BigDecimal("4.40"))
                .heightCm(new BigDecimal("58.00"))
                .build());

        saved.setDeletedAt(Instant.now());
        store.save(saved);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ? AND deleted_at IS NULL
                """, Long.class, saved.getGrowthMeasurementId()))
                .isZero();
        assertThat(store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(baby))
                .extracting(GrowthMeasurement::getGrowthMeasurementId)
                .doesNotContain(saved.getGrowthMeasurementId());
        // History that deliberately includes deleted sessions must still find it.
        assertThat(store.findByBabyIdOrderByMeasuredDateAsc(baby))
                .extracting(GrowthMeasurement::getGrowthMeasurementId)
                .contains(saved.getGrowthMeasurementId());
    }

    @Test
    void listsAreOrderedByMeasuredDateAndPaginateBothWays() {
        UUID baby = seedBaby();
        store.save(session(baby, LocalDate.of(2026, 6, 1), new BigDecimal("3.10")));
        store.save(session(baby, LocalDate.of(2026, 7, 1), new BigDecimal("3.80")));
        store.save(session(baby, LocalDate.of(2026, 8, 1), new BigDecimal("4.40")));

        assertThat(store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(baby))
                .extracting(GrowthMeasurement::getMeasuredDate)
                .containsExactly(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 1));

        var firstPage = store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(
                baby, PageRequest.of(0, 2));
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(GrowthMeasurement::getMeasuredDate)
                .containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1));
        assertThat(store.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(
                        baby, PageRequest.of(1, 2)).getContent())
                .extracting(GrowthMeasurement::getMeasuredDate)
                .containsExactly(LocalDate.of(2026, 6, 1));
    }

    @Test
    void aSessionBelongingToAnotherBabyIsNotReachable() {
        UUID babyA = seedBaby();
        UUID babyB = seedBaby();
        GrowthMeasurement ofA = store.save(session(babyA, LocalDate.of(2026, 7, 9),
                new BigDecimal("5.00")));

        assertThat(store.findByGrowthMeasurementIdAndBabyId(ofA.getGrowthMeasurementId(), babyB))
                .isEmpty();
        assertThat(store.findByGrowthMeasurementIdAndBabyId(ofA.getGrowthMeasurementId(), babyA))
                .isPresent();
    }

    // COVERAGE RETIRED (wave 13 contract, V20260807160000): a test here replayed the backfill
    // migration into growth_measurements and read the result back through this store, proving
    // the two agreed on grouping, units and where the note lives. The source table is gone, so
    // the scenario is unreachable. What it protected was verified against the live database
    // before the drop: 24 of 24 measurements migrated, 0 diverging, and all 8 sessions
    // projected back identically to their source rows.

    private List<String> observationTypes(UUID group) {
        return jdbcTemplate.queryForList("""
                SELECT observation_type FROM health_observations
                 WHERE measurement_group_id = ? ORDER BY observation_type
                """, String.class, group);
    }

    private static GrowthMeasurement session(UUID baby, LocalDate on, BigDecimal weight) {
        return GrowthMeasurement.builder().babyId(baby).measuredDate(on).weightKg(weight).build();
    }

    private UUID seedBaby() {
        UUID subjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'BABY', 'Growth store probe', 'ACTIVE', now(), now()
                  FROM users u ORDER BY u.created_at LIMIT 1
                """, subjectId);
        return subjectId;
    }
}
