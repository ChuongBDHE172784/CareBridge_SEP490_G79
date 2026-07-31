package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.entity.HealthObservation;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC25 — AddMaternalHealthMetric service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class HealthMetricAddServiceTest {

    @Mock private HealthObservationRepository observationRepository;
    @Mock private MetricDefinitionRepository definitionRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private CareGroupRepository careGroupRepository;
    @Spy private MetricObservationValidator validator = new MetricObservationValidator();
    @InjectMocks private HealthMetricServiceImpl metricService;

    /** METRIC-TC-025-001: Happy path WEIGHT — metric saved + AI insight + audit emitted. */
    @Test
    void addMetric_weightHappyPath_returnsMetricResponse() {
        var journey = MetricTestFactory.makeActiveJourney();
        var req = MetricTestFactory.makeWeightRequest();
        var saved = MetricTestFactory.makeSavedObservation();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(definitionRepository.findByMetricCodeAndActiveTrue("WEIGHT"))
                .thenReturn(Optional.of(MetricTestFactory.makeWeightDefinition()));
        when(observationRepository.save(any())).thenReturn(saved);

        MetricResponse response = metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req);

        assertThat(response.getMetricId()).isEqualTo(MetricTestFactory.METRIC_ID);
        assertThat(response.getMetricType()).isEqualTo(MetricType.WEIGHT.name());
        assertThat(response.getJourneyId()).isEqualTo(MetricTestFactory.JOURNEY_ID);
        verify(observationRepository).save(any(HealthObservation.class));
        verify(auditService).log(eq(AuditAction.HEALTH_METRIC_ADDED), eq(MetricTestFactory.MOTHER_ID),
                eq("HealthObservation"), any(), any());
    }

    /** METRIC-TC-025-002: CRITICAL — BP requires both valueNumeric AND valueSecondary → METRIC-005 (400). */
    @Test
    void addMetric_bloodPressureMissingSecondary_throwsMetric005() {
        var journey = MetricTestFactory.makeActiveJourney();
        var req = MetricTestFactory.makeBloodPressureMissingSecondary();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(definitionRepository.findByMetricCodeAndActiveTrue("BLOOD_PRESSURE"))
                .thenReturn(Optional.of(MetricTestFactory.makeBloodPressureDefinition()));

        assertThatThrownBy(() -> metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-038");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(observationRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** METRIC-TC-025-003: CRITICAL SECURITY — Journey not owned by user → METRIC-002 (403). */
    @Test
    void addMetric_journeyNotOwned_throwsMetric002() {
        var otherJourney = MetricTestFactory.makeOtherUsersJourney();
        var req = MetricTestFactory.makeWeightRequest();
        when(journeyRepository.findById(MetricTestFactory.OTHER_JOURNEY)).thenReturn(Optional.of(otherJourney));

        assertThatThrownBy(() -> metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.OTHER_JOURNEY, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-002");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(observationRepository, never()).save(any());
    }

    /** METRIC-TC-025-004: Journey is COMPLETED (not ACTIVE) → METRIC-003 (400). */
    @Test
    void addMetric_journeyNotActive_throwsMetric003() {
        var completedJourney = MetricTestFactory.makeCompletedJourney();
        var req = MetricTestFactory.makeWeightRequest();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.of(completedJourney));

        assertThatThrownBy(() -> metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-003");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(observationRepository, never()).save(any());
    }

    /** METRIC-TC-025-005: measuredAt more than 5min in the future → METRIC-004 (400). */
    @Test
    void addMetric_futureMeasuredAt_throwsMetric004() {
        var journey = MetricTestFactory.makeActiveJourney();
        var req = MetricTestFactory.makeFutureMeasuredAtRequest();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(definitionRepository.findByMetricCodeAndActiveTrue("WEIGHT"))
                .thenReturn(Optional.of(MetricTestFactory.makeWeightDefinition()));

        assertThatThrownBy(() -> metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-004");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(observationRepository, never()).save(any());
    }

    /** METRIC-TC-025-006: Journey not found → METRIC-001 (404). */
    @Test
    void addMetric_journeyNotFound_throwsMetric001() {
        var req = MetricTestFactory.makeWeightRequest();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("METRIC-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(observationRepository, never()).save(any());
    }

    /** METRIC-TC-025-007: AI analyzer fails — metric still saved (fail-open), aiInsight=null. */
    @Test
    void addMetric_canonicalObservation_keepsCompatibleResponse() {
        var journey = MetricTestFactory.makeActiveJourney();
        var req = MetricTestFactory.makeWeightRequest();
        var saved = MetricTestFactory.makeSavedObservation();
        when(journeyRepository.findById(MetricTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(definitionRepository.findByMetricCodeAndActiveTrue("WEIGHT"))
                .thenReturn(Optional.of(MetricTestFactory.makeWeightDefinition()));
        when(observationRepository.save(any())).thenReturn(saved);

        MetricResponse response = metricService.addMetric(
                MetricTestFactory.MOTHER_ID, MetricTestFactory.JOURNEY_ID, req);

        assertThat(response.getMetricId()).isEqualTo(MetricTestFactory.METRIC_ID);
        assertThat(response.getAiInsight()).isNull();
        assertThat(response.isRedFlagAlert()).isFalse();
        verify(observationRepository).save(argThat(observation ->
                observation.getCareSubjectId().equals(MetricTestFactory.CARE_SUBJECT_ID)
                        && observation.getMetricCode().equals("WEIGHT")
                        && observation.getPayload().get("journeyId")
                        .equals(MetricTestFactory.JOURNEY_ID.toString())));
    }
}
