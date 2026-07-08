package com.carebridge.backend.health;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.MetricTrendResponse;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC27 — ViewMaternalHealthTrend service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class MetricTrendServiceTest {

    @Mock private MaternalHealthMetricRepository metricRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private MetricAiAnalyzer metricAiAnalyzer;
    @InjectMocks private HealthMetricServiceImpl metricService;

    private static final Instant FROM = Instant.now().minus(90, ChronoUnit.DAYS);
    private static final Instant TO   = Instant.now();

    /** METRIC-TC-027-001: Happy path — 5 WEIGHT metrics returned, sorted ASC. */
    @Test
    void getMetricTrend_weightMetrics_returnsDataPointsSortedAsc() {
        var journey = MetricTrendTestFactory.makeJourney();
        var metrics = MetricTrendTestFactory.makeWeightMetrics(5);
        when(journeyRepository.findById(MetricTrendTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(MetricTrendTestFactory.JOURNEY_ID), eq(MetricType.WEIGHT), eq(MetricStatus.ACTIVE),
                any(Instant.class), any(Instant.class)))
                .thenReturn(metrics);

        MetricTrendResponse response = metricService.getMetricTrend(
                MetricTrendTestFactory.MOTHER_ID, MetricTrendTestFactory.JOURNEY_ID,
                MetricType.WEIGHT, FROM, TO);

        assertThat(response.getMetricType()).isEqualTo(MetricType.WEIGHT.name());
        assertThat(response.getDataPoints()).hasSize(5);
        // Verify sorted ASC by measuredAt
        List<Instant> timestamps = response.getDataPoints().stream()
                .map(dp -> dp.getMeasuredAt())
                .toList();
        for (int i = 0; i < timestamps.size() - 1; i++) {
            assertThat(timestamps.get(i)).isBeforeOrEqualTo(timestamps.get(i + 1));
        }
    }

    /** METRIC-TC-027-002: CRITICAL — No data in range → 200 OK with empty list (never 404). */
    @Test
    void getMetricTrend_noData_returns200WithEmptyList() {
        var journey = MetricTrendTestFactory.makeJourney();
        when(journeyRepository.findById(MetricTrendTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() -> {
            MetricTrendResponse response = metricService.getMetricTrend(
                    MetricTrendTestFactory.MOTHER_ID, MetricTrendTestFactory.JOURNEY_ID,
                    MetricType.WEIGHT, FROM, TO);
            assertThat(response.getDataPoints()).isEmpty();
        });
    }

    /** METRIC-TC-027-003: CRITICAL SECURITY — Journey not owned → METRIC-021 (403). */
    @Test
    void getMetricTrend_journeyNotOwned_throwsMetric021() {
        var otherJourney = MetricTrendTestFactory.makeOtherUsersJourney();
        when(journeyRepository.findById(MetricTrendTestFactory.JOURNEY_ID)).thenReturn(Optional.of(otherJourney));

        assertThatThrownBy(() -> metricService.getMetricTrend(
                MetricTrendTestFactory.MOTHER_ID, MetricTrendTestFactory.JOURNEY_ID,
                MetricType.WEIGHT, FROM, TO))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-021");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    /** METRIC-TC-027-004: Journey not found → METRIC-020 (404). */
    @Test
    void getMetricTrend_journeyNotFound_throwsMetric020() {
        when(journeyRepository.findById(MetricTrendTestFactory.JOURNEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.getMetricTrend(
                MetricTrendTestFactory.MOTHER_ID, MetricTrendTestFactory.JOURNEY_ID,
                MetricType.WEIGHT, FROM, TO))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-020");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    /** METRIC-TC-027-005: BP metrics have both valueNumeric AND valueSecondary populated. */
    @Test
    void getMetricTrend_bloodPressureMetrics_returnsBothValues() {
        var journey = MetricTrendTestFactory.makeJourney();
        var bpMetrics = MetricTrendTestFactory.makeBloodPressureMetrics(3);
        when(journeyRepository.findById(MetricTrendTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(metricRepository.findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                any(), eq(MetricType.BLOOD_PRESSURE_DIASTOLIC), any(), any(), any()))
                .thenReturn(bpMetrics);

        MetricTrendResponse response = metricService.getMetricTrend(
                MetricTrendTestFactory.MOTHER_ID, MetricTrendTestFactory.JOURNEY_ID,
                MetricType.BLOOD_PRESSURE_DIASTOLIC, FROM, TO);

        assertThat(response.getDataPoints()).hasSize(3);
        response.getDataPoints().forEach(dp -> {
            assertThat(dp.getValueNumeric()).isNotNull();
            assertThat(dp.getValueSecondary()).isNotNull();
        });
    }
}
