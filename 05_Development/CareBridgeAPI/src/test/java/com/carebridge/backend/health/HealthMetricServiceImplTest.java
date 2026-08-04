package com.carebridge.backend.health;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.health.event.MaternalHealthMetricDeleted;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.health.repository.MetricDefinitionRepository;
import com.carebridge.backend.health.service.MetricObservationValidator;
import com.carebridge.backend.health.service.impl.HealthMetricServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthMetricServiceImplTest {

    @Mock private HealthObservationRepository observationRepository;
    @Mock private MetricDefinitionRepository definitionRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private CareGroupRepository careGroupRepository;
    @Mock private CareGroupAuthorizationPolicy careGroupAuthorizationPolicy;
    @Mock private MetricObservationValidator validator;
    @InjectMocks private HealthMetricServiceImpl metricService;

    private static final UUID CALLER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID METRIC_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CARE_SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    private HealthObservation makeMetric() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("journeyId", JOURNEY_ID.toString());
        payload.put("recordStatus", MetricStatus.ACTIVE.name());
        return HealthObservation.builder()
                .id(METRIC_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .metricCode("WEIGHT")
                .valueNumeric(new BigDecimal("65.5"))
                .unit("kg")
                .measuredAt(Instant.now())
                .sourceType(DataSource.MANUAL)
                .definitionVersion(1)
                .observationShape(ObservationShape.POINT)
                .qualityLabel("UNKNOWN")
                .context(new LinkedHashMap<>())
                .payload(payload)
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .legacyId(METRIC_ID.toString())
                .subjectType("MOTHER")
                .build();
    }

    private MotherJourney makeJourney(UUID ownerId) {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(ownerId)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    private MetricDefinition weightDefinition() {
        return definition("WEIGHT", ObservationShape.POINT, "kg", true);
    }

    private MetricDefinition definition(String metricCode, ObservationShape shape, String unit, boolean manualEntry) {
        return MetricDefinition.builder()
                .metricCode(metricCode)
                .version(1)
                .displayName(metricCode)
                .observationShape(shape)
                .subjectType("MOTHER")
                .manualEntrySupported(manualEntry)
                .canonicalUnit(unit)
                .acceptedInputUnits(java.util.List.of(unit))
                .precisionScale((short) 2)
                .active(true)
                .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    // METRIC-TC-001: Owner can view active metric
    @Test
    void getMetricDetail_ownerAccess_returnsDetail() {
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
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
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
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
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricService.getMetricDetail(METRIC_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // METRIC-TC-004: C1 — non-owner → 403
    @Test
    void getMetricDetail_notOwner_throwsBusinessException403() {
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(makeMetric()));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(UUID.randomUUID()))); // different owner

        assertThatThrownBy(() -> metricService.getMetricDetail(METRIC_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteMetric_ownerSoftDeletesMetric() {
        HealthObservation metric = makeMetric();
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(metric));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(CALLER_ID)));

        metricService.deleteMetric(METRIC_ID, CALLER_ID);

        assertThat(metric.recordStatus()).isEqualTo(MetricStatus.DELETED);
        verify(observationRepository).save(metric);
        verify(observationRepository).updateStatus(METRIC_ID, MetricStatus.DELETED);
        verify(observationRepository, never()).delete(any());
        verify(observationRepository, never()).deleteById(any());
        verify(eventPublisher).publishEvent(any(MaternalHealthMetricDeleted.class));
    }

    @Test
    void addMetric_persistsJourneyCareSubjectInsteadOfJourneyId() {
        MotherJourney journey = makeJourney(CALLER_ID);
        AddMetricRequest request = new AddMetricRequest();
        request.setMetricType(MetricType.WEIGHT);
        request.setValueNumeric(new BigDecimal("65.5"));
        request.setUnit("kg");
        request.setMeasuredAt(Instant.now().minusSeconds(30));

        MetricObservationValidator.NormalizedObservation normalized =
                new MetricObservationValidator.NormalizedObservation(
                        "WEIGHT",
                        new BigDecimal("65.50"),
                        null,
                        "kg",
                        request.getMeasuredAt(),
                        DataSource.MANUAL,
                        null,
                        new LinkedHashMap<>(),
                        null,
                        null,
                        1);

        when(journeyRepository.findById(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(validator.canonicalCode(MetricType.WEIGHT)).thenReturn("WEIGHT");
        MetricDefinition definition = weightDefinition();
        when(definitionRepository.findByMetricCodeAndActiveTrue("WEIGHT"))
                .thenReturn(Optional.of(definition));
        when(validator.normalize(request, definition)).thenReturn(normalized);
        when(observationRepository.save(any(HealthObservation.class)))
                .thenAnswer(invocation -> {
                    HealthObservation saved = invocation.getArgument(0);
                    saved.setId(METRIC_ID);
                    return saved;
                });

        metricService.addMetric(CALLER_ID, JOURNEY_ID, request);

        var captor = org.mockito.ArgumentCaptor.forClass(HealthObservation.class);
        verify(observationRepository).save(captor.capture());
        HealthObservation saved = captor.getValue();
        assertThat(saved.getCareSubjectId()).isEqualTo(CARE_SUBJECT_ID);
        assertThat(saved.getCareSubjectId()).isNotEqualTo(JOURNEY_ID);
        assertThat(saved.getPayload()).containsEntry("journeyId", JOURNEY_ID.toString());
    }

    @Test
    void getCapabilities_exposesOnlySupportedP0ManualMetrics() {
        MotherJourney journey = makeJourney(CALLER_ID);
        MetricDefinition bmi = definition("BMI", ObservationShape.POINT, "kg/m²", true);
        MetricDefinition bloodPressure = definition("BLOOD_PRESSURE", ObservationShape.PAIRED_POINT, "mmHg", true);
        MetricDefinition bloodGlucose = definition("BLOOD_GLUCOSE", ObservationShape.POINT, "mg/dL", true);
        MetricDefinition fetalMovement = definition("FETAL_MOVEMENT_SESSION", ObservationShape.SESSION, "count", true);
        MetricDefinition temperature = definition("TEMPERATURE", ObservationShape.POINT, "Cel", true);
        MetricDefinition steps = definition("STEPS", ObservationShape.INTERVAL_AGGREGATE, "count", false);

        when(journeyRepository.findById(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(definitionRepository.findAllEffectiveAt(any(Instant.class)))
                .thenReturn(java.util.List.of(
                        bmi, bloodPressure, bloodGlucose, fetalMovement, temperature, steps));

        var capabilities = metricService.getCapabilities(JOURNEY_ID, CALLER_ID);

        assertThat(capabilities)
                .extracting("metricCode")
                .containsExactly("BMI", "BLOOD_PRESSURE", "BLOOD_GLUCOSE", "FETAL_MOVEMENT_SESSION");
    }

    @Test
    void familyTrend_requiresLinkedGroupRecordsPermission() {
        MotherJourney journey = makeJourney(CALLER_ID);
        CareGroupMember membership = CareGroupMember.builder()
                .careGroupId(GROUP_ID)
                .userId(FAMILY_ID)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build();
        CareGroup group = CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(CALLER_ID)
                .linkedJourneyId(JOURNEY_ID)
                .status(CareGroupStatus.ACTIVE)
                .build();

        when(journeyRepository.findById(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(careGroupMemberRepository.findByUserIdAndInviteStatus(FAMILY_ID, InviteStatus.ACCEPTED))
                .thenReturn(java.util.List.of(membership));
        when(careGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(careGroupAuthorizationPolicy.hasPermission(GROUP_ID, FAMILY_ID, PermissionFlag.RECORDS))
                .thenReturn(false);

        assertThatThrownBy(() -> metricService.getMetricTrend(
                FAMILY_ID,
                JOURNEY_ID,
                MetricType.WEIGHT,
                Instant.now().minusSeconds(3600),
                Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(observationRepository, never()).findTrend(any(), any(), any(), any(), any());
    }

    @Test
    void deleteMetric_notOwnerDoesNotMutate() {
        HealthObservation metric = makeMetric();
        when(observationRepository.findByIdAndStatus(METRIC_ID, MetricStatus.ACTIVE))
                .thenReturn(Optional.of(metric));
        when(journeyRepository.findById(JOURNEY_ID))
                .thenReturn(Optional.of(makeJourney(UUID.randomUUID())));

        assertThatThrownBy(() -> metricService.deleteMetric(METRIC_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(metric.recordStatus()).isEqualTo(MetricStatus.ACTIVE);
        verify(observationRepository, never()).save(any());
        verify(observationRepository, never()).updateStatus(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
