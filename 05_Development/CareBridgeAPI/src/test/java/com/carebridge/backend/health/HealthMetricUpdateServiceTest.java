package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.health.repository.MetricDefinitionRepository;
import com.carebridge.backend.health.service.MetricObservationValidator;
import com.carebridge.backend.health.service.impl.HealthMetricServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock private HealthObservationRepository observationRepository;
    @Mock private MetricDefinitionRepository definitionRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private CareGroupRepository careGroupRepository;
    @Spy private MetricObservationValidator validator = new MetricObservationValidator();
    @InjectMocks private HealthMetricServiceImpl metricService;

    /** METRIC-TC-026-001: Happy path — update value within 24h window; metricType unchanged. */
    @Test
    void updateMetric_withinEditWindow_returnsUpdatedResponse() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var metric = MetricUpdateTestFactory.makeRecentObservation(); // created 1h ago
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(observationRepository.findByIdAndCareSubjectIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.CARE_SUBJECT_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(metric));
        when(definitionRepository.findByMetricCodeAndActiveTrue("WEIGHT"))
                .thenReturn(Optional.of(MetricUpdateTestFactory.makeWeightDefinition()));
        when(observationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MetricResponse response = metricService.updateMetric(
                MetricUpdateTestFactory.MOTHER_ID, MetricUpdateTestFactory.JOURNEY_ID,
                MetricUpdateTestFactory.METRIC_ID, req);

        assertThat(response.getValueNumeric()).isEqualByComparingTo(new BigDecimal("66.0"));
        assertThat(response.getMetricType()).isEqualTo(MetricType.WEIGHT.name());
        assertThat(response.getNote()).isEqualTo("Corrected weight measurement");
        verify(auditService).log(eq(AuditAction.HEALTH_METRIC_UPDATED), eq(MetricUpdateTestFactory.MOTHER_ID),
                eq("HealthObservation"), any(), any());
        verify(observationRepository).save(any(HealthObservation.class));
    }

    /** METRIC-TC-026-002: CRITICAL — Edit window expired (25h old) → METRIC-012 (400). */
    @Test
    void updateMetric_editWindowExpired_throwsMetric012() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var oldMetric = MetricUpdateTestFactory.makeOldObservation(); // created 25h ago
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(observationRepository.findByIdAndCareSubjectIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.CARE_SUBJECT_ID, MetricStatus.ACTIVE))
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
        verify(observationRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** METRIC-TC-026-003: Metric not in this journey → METRIC-011 (404). */
    @Test
    void updateMetric_metricNotInJourney_throwsMetric011() {
        var journey = MetricUpdateTestFactory.makeActiveJourney();
        var req = MetricUpdateTestFactory.makeUpdateWeightRequest();
        when(journeyRepository.findById(MetricUpdateTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(observationRepository.findByIdAndCareSubjectIdAndStatus(
                MetricUpdateTestFactory.METRIC_ID, MetricUpdateTestFactory.CARE_SUBJECT_ID, MetricStatus.ACTIVE))
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
        verify(observationRepository, never()).save(any());
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
        verify(observationRepository, never()).save(any());
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
        verify(observationRepository, never()).save(any());
    }
}
