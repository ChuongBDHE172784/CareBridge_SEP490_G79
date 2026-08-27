package com.carebridge.backend.baby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.service.BabyBirthGrowthSynchronizer;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementStore;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Proves the birth projection reaches the real PostgreSQL-backed Growth read model. */
class BabyBirthGrowthSynchronizerEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private BabyBirthGrowthSynchronizer synchronizer;
    @Autowired private GrowthMeasurementStore growthMeasurementStore;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void projectionCreatesOneBabyGrowthSessionAndReplayDoesNotDuplicateRows() {
        UUID babyId = seedBaby();
        BabyProfile profile = BabyProfile.builder()
                .id(babyId)
                .birthDate(LocalDate.of(2026, 8, 1))
                .birthWeightKg(new BigDecimal("3.25"))
                .birthLengthCm(new BigDecimal("50.0"))
                .build();

        synchronizer.synchronize(profile);
        synchronizer.synchronize(profile);

        UUID groupId = BabyBirthGrowthSynchronizer.deterministicSessionId(babyId);
        assertThat(growthMeasurementStore.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId))
                .singleElement()
                .satisfies(session -> {
                    assertThat(session.getGrowthMeasurementId()).isEqualTo(groupId);
                    assertThat(session.getMeasuredDate()).isEqualTo(LocalDate.of(2026, 8, 1));
                    assertThat(session.getWeightKg()).isEqualByComparingTo("3.25");
                    assertThat(session.getHeightCm()).isEqualByComparingTo("50.0");
                    assertThat(session.getSourceType())
                            .isEqualTo(BabyBirthGrowthSynchronizer.BIRTH_RECORD_SOURCE);
                });
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ?
                   AND legacy_source = 'growth_measurements'
                """, Long.class, groupId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ?
                   AND subject_type = 'BABY'
                   AND care_subject_id = ?
                """, Long.class, groupId, babyId)).isEqualTo(2L);
    }

    @Test
    void replayFailsClosedWhenExistingRowsBelongToAnotherSubject() {
        UUID babyId = seedBaby();
        UUID otherBabyId = seedBaby();
        BabyProfile profile = BabyProfile.builder()
                .id(babyId)
                .birthDate(LocalDate.of(2026, 8, 1))
                .birthWeightKg(new BigDecimal("3.25"))
                .build();

        synchronizer.synchronize(profile);
        UUID groupId = BabyBirthGrowthSynchronizer.deterministicSessionId(babyId);
        assertThat(growthMeasurementStore.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId))
                .singleElement()
                .satisfies(session -> {
                    assertThat(session.getWeightKg()).isEqualByComparingTo("3.25");
                    assertThat(session.getHeightCm()).isNull();
                });
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ?
                   AND legacy_source = 'growth_measurements'
                """, Long.class, groupId)).isEqualTo(1L);
        jdbcTemplate.update("""
                UPDATE health_observations
                   SET care_subject_id = ?
                 WHERE measurement_group_id = ?
                   AND legacy_source = 'growth_measurements'
                """, otherBabyId, groupId);

        assertThatThrownBy(() -> synchronizer.synchronize(profile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Growth measurement identity mismatch");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM health_observations
                 WHERE measurement_group_id = ?
                   AND care_subject_id = ?
                """, Long.class, groupId, otherBabyId)).isEqualTo(1L);
    }

    private UUID seedBaby() {
        UUID subjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'BABY', 'Birth projection probe', 'ACTIVE', now(), now()
                  FROM users u ORDER BY u.created_at LIMIT 1
                """, subjectId);
        return subjectId;
    }
}
