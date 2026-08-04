package com.carebridge.backend.health.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class HealthObservationRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private HealthObservationRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void familySummaryQueriesUseCanonicalActiveSubjectScopedObservationsAndInclusiveBounds() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID otherSubjectId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, ownerId, "Metric owner", null, "MOTHER");
        CanonicalUserFixture.insertUser(jdbcTemplate, otherOwnerId, "Other owner", null, "MOTHER");
        insertSubject(subjectId, ownerId);
        insertSubject(otherSubjectId, otherOwnerId);

        Instant from = Instant.parse("2026-08-03T00:00:00Z");
        Instant to = Instant.parse("2026-08-03T23:59:59Z");
        HealthObservation legacySource =
                observation(subjectId, "WEIGHT", "77", from.plusSeconds(300), "ACTIVE");
        legacySource.setLegacySource("maternal_health_metrics");
        repository.saveAll(List.of(
                observation(subjectId, "WEIGHT", "60.0", from, "ACTIVE"),
                observation(subjectId, "WEIGHT", "61.5", from.plusSeconds(60), "ACTIVE"),
                observation(subjectId, "HYDRATION", "250", to, "ACTIVE"),
                observation(subjectId, "HYDRATION", "999", from.plusSeconds(120), "DELETED"),
                observation(subjectId, "BLOOD_GLUCOSE", "95", from.plusSeconds(180), "ACTIVE"),
                observation(otherSubjectId, "WEIGHT", "88", from.plusSeconds(240), "ACTIVE"),
                legacySource));
        repository.flush();

        var latest = repository.findLatestByMetricCodes(
                subjectId, List.of("WEIGHT", "HYDRATION"), MetricStatus.ACTIVE);
        assertThat(latest).extracting(HealthObservation::getMetricCode)
                .containsExactlyInAnyOrder("WEIGHT", "HYDRATION");
        assertThat(latest.stream()
                .filter(item -> "WEIGHT".equals(item.getMetricCode()))
                .findFirst().orElseThrow().getValueNumeric())
                .isEqualByComparingTo("61.5");

        var trend = repository.findTrendByMetricCodes(
                subjectId, List.of("WEIGHT", "HYDRATION"), MetricStatus.ACTIVE, from, to);
        assertThat(trend).extracting(HealthObservation::getValueNumeric)
                .containsExactly(
                        new BigDecimal("60.00"),
                        new BigDecimal("61.50"),
                        new BigDecimal("250.00"));
    }

    private void insertSubject(UUID subjectId, UUID ownerId) {
        jdbcTemplate.update(
                "INSERT INTO care_subjects "
                        + "(care_subject_id, person_id, owner_user_id, subject_type, status) "
                        + "VALUES (?, ?, ?, 'MOTHER', 'ACTIVE')",
                subjectId, ownerId, ownerId);
    }

    private HealthObservation observation(
            UUID subjectId,
            String metricCode,
            String value,
            Instant measuredAt,
            String recordStatus) {
        return HealthObservation.builder()
                .careSubjectId(subjectId)
                .metricCode(metricCode)
                .valueNumeric(new BigDecimal(value))
                .unit("HYDRATION".equals(metricCode) ? "ml" : "kg")
                .measuredAt(measuredAt)
                .sourceType(DataSource.MANUAL)
                .context(Map.of())
                .payload(Map.of("recordStatus", recordStatus))
                .build();
    }
}
