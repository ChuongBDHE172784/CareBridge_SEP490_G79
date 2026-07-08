package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.health.service.MetricAiAnalyzer;
import com.carebridge.backend.health.service.impl.HealthMetricServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC26 — UpdateMaternalHealthMetric service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class HealthMetricUpdateServiceTest {

    @Mock private MaternalHealthMetricRepository metricRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private MetricAiAnalyzer metricAiAnalyzer;
    @InjectMocks private HealthMetricServiceImpl metricService;

    /** METRIC-TC-026-001: Happy path — update value within 24h window; metricType unchanged. */
    @Test
    void updateMetric_withinEditWindow_returnsUpdatedResponse() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var metric = MetricUpdateTestFactory.makeRecentMetric(); // created 1h ago
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByIdAndJourneyIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.JOURNEY_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(metric));
        when(metricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MetricResponse response = metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req);

        assertThat(response.getValueNumeric()).isEqualByComparingTo(new BigDecimal("66.0"));
        assertThat(response.getMetricType()).isEqualTo(MetricType.WEIGHT.name());
        assertThat(response.getNote()).isEqualTo("Corrected weight measurement");
        verify(auditService).log(eq(AuditAction.HEALTH_METRIC_UPDATED), eq(MetricUpdateTestFactory.MOTHER_ID),
                eq("MaternalHealthMetric"), any(), any());
        verify(metricRepository).save(any());
    }

    /** METRIC-TC-026-002: CRITICAL — Edit window expired (25h old) → METRIC-012 (400). */
    @Test
    void updateMetric_editWindowExpired_throwsMetric012() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var oldMetric = MetricUpdateTestFactory.makeOldMetric(); // created 25h ago
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByIdAndJourneyIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.JOURNEY_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(oldMetric));

        assertThatThrownBy(() -> metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-012");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(metricRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** METRIC-TC-026-003: Metric not in this journey → METRIC-011 (404). */
    @Test
    void updateMetric_metricNotInJourney_throwsMetric011() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByIdAndJourneyIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.JOURNEY_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-011");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(metricRepository, never()).save(any());
    }

    /** METRIC-TC-026-004: CRITICAL SECURITY — Journey owned by different user → METRIC-013 (403). */
    @Test
    void updateMetric_journeyNotOwned_throwsMetric013() {
        var otherJourney = MetricUpdateTestFactory.makeOtherUsersJourney();
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(otherJourney));

        assertThatThrownBy(() -> metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-013");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(metricRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** METRIC-TC-026-005: Journey not found → METRIC-010 (404). */
    @Test
    void updateMetric_journeyNotFound_throwsMetric010() {
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-010");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(metricRepository, never()).save(any());
    }
}
