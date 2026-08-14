package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.health.event.EpdsScreeningCompleted;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.health.repository.MetricDefinitionRepository;
import com.carebridge.backend.health.service.MetricObservationValidator;
import com.carebridge.backend.health.service.impl.HealthMetricServiceImpl;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * CB-EPDS-TEST-001 — TC-18, TC-19.
 *
 * <p>Verifies {@code addMetric} publishes {@link EpdsScreeningCompleted} only for EPDS_SCORE.
 */
@ExtendWith(MockitoExtension.class)
class HealthMetricServiceEpdsEventTest {

    @Mock private HealthObservationRepository observationRepository;
    @Mock private MetricDefinitionRepository definitionRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MetricObservationValidator validator;
    @InjectMocks private HealthMetricServiceImpl metricService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID METRIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CARE_SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private MotherJourney makeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(CALLER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    private MetricDefinition definition(String code) {
        return MetricDefinition.builder()
                .metricCode(code)
                .version(1)
                .observationShape(ObservationShape.POINT)
                .active(true)
                .build();
    }

    /** Drives addMetric end-to-end for one metric code, returning nothing but exercising the publish. */
    private void submit(MetricType type, String code, BigDecimal value, BigDecimal secondary) {
        AddMetricRequest request = new AddMetricRequest();
        request.setMetricType(type);
        request.setValueNumeric(value);
        request.setUnit("điểm");
        request.setMeasuredAt(Instant.now().minusSeconds(30));

        MetricObservationValidator.NormalizedObservation normalized =
                new MetricObservationValidator.NormalizedObservation(
                        code, value, secondary, "điểm", request.getMeasuredAt(),
                        DataSource.MANUAL, null, new LinkedHashMap<>(), null, null, 1);

        when(journeyRepository.findById(JOURNEY_ID)).thenReturn(Optional.of(makeJourney()));
        when(validator.canonicalCode(type)).thenReturn(code);
        MetricDefinition def = definition(code);
        when(definitionRepository.findByMetricCodeAndActiveTrue(code)).thenReturn(Optional.of(def));
        when(validator.normalize(request, def)).thenReturn(normalized);
        when(observationRepository.save(any(HealthObservation.class)))
                .thenAnswer(invocation -> {
                    HealthObservation saved = invocation.getArgument(0);
                    saved.setId(METRIC_ID);
                    return saved;
                });

        metricService.addMetric(CALLER_ID, JOURNEY_ID, request);
    }

    // ---------------------------------------------------------------- TC-18
    @Test
    void epdsSubmissionPublishesEventWithScoreAndQuestion10() {
        submit(MetricType.EPDS_SCORE, "EPDS_SCORE", new BigDecimal("12"), new BigDecimal("3"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(EpdsScreeningCompleted.class);
        EpdsScreeningCompleted event = (EpdsScreeningCompleted) captor.getValue();
        assertThat(event.motherUserId()).isEqualTo(CALLER_ID);
        assertThat(event.journeyId()).isEqualTo(JOURNEY_ID);
        assertThat(event.observationId()).isEqualTo(METRIC_ID);
        assertThat(event.totalScore()).isEqualTo(12);
        assertThat(event.question10Score()).isEqualTo(3);
    }

    @Test
    void nonEpdsMetricPublishesNoEpdsEvent() {
        submit(MetricType.WEIGHT, "WEIGHT", new BigDecimal("65.5"), null);

        verify(eventPublisher, never()).publishEvent(any(EpdsScreeningCompleted.class));
    }

    // ---------------------------------------------------------------- TC-19
    @Test
    void nullValueSecondaryIsTreatedAsNonEscalating() {
        submit(MetricType.EPDS_SCORE, "EPDS_SCORE", new BigDecimal("8"), null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());

        EpdsScreeningCompleted event = (EpdsScreeningCompleted) captor.getValue();
        assertThat(event.totalScore()).isEqualTo(8);
        assertThat(event.question10Score()).isZero();
    }
}
