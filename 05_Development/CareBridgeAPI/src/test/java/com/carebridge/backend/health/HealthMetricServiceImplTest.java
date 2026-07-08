package com.carebridge.backend.health;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthMetricServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthMetricServiceImplTest {

    @Mock private MaternalHealthMetricRepository metricRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @InjectMocks private HealthMetricServiceImpl metricService;

    private static final UUID CALLER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID METRIC_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private MaternalHealthMetric makeMetric() {
        return MaternalHealthMetric.builder()
                .id(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("65.5"))
                .unit("kg")
                .measuredAt(Instant.now())
                .status(MetricStatus.ACTIVE)
                .build();
    }

    private MotherJourney makeJourney(UUID ownerId) {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(ownerId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    // METRIC-TC-001: Owner can view active metric
    @Test
    void getMetricDetail_ownerAccess_returnsDetail() {
        when(metricRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(makeMetric()));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(CALLER_ID)));

        MetricDetailResponse resp = metricService.getMetricDetail(METRIC_ID, CALLER_ID);

        assertThat(resp.getId()).isEqualTo(METRIC_ID);
        assertThat(resp.getMetricType()).isEqualTo("WEIGHT");
    }

    // METRIC-TC-002: C2 — no diagnosis in response (BR-SAFETY)
    @Test
    void getMetricDetail_noDiagnosisInResponse() {
        when(metricRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(makeMetric()));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(CALLER_ID)));

        MetricDetailResponse resp = metricService.getMetricDetail(METRIC_ID, CALLER_ID);

        assertThat(resp.toString()).doesNotContainIgnoringCase("diagnosis");
        assertThat(resp.toString()).doesNotContainIgnoringCase("treatment");
    }

    // METRIC-TC-003: DELETED metric → 404
    @Test
    void getMetricDetail_deletedMetric_throwsBusinessException404() {
        when(metricRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.getMetricDetail(METRIC_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // METRIC-TC-004: C1 — non-owner → 403
    @Test
    void getMetricDetail_notOwner_throwsBusinessException403() {
        when(metricRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(makeMetric()));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(UUID.randomUUID()))); // different owner

        assertThatThrownBy(() -> metricService.getMetricDetail(METRIC_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
