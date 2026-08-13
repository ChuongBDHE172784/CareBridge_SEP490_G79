package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.MetricCapabilityResponse;
import com.carebridge.backend.health.dto.MetricDataPoint;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.event.MaternalHealthMetricDeleted;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.health.repository.MetricDefinitionRepository;
import com.carebridge.backend.health.service.IHealthMetricService;
import com.carebridge.backend.health.service.MetricObservationValidator;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements IHealthMetricService {

    private static final java.util.Set<String> P0_MANUAL_METRICS = java.util.Set.of(
            "BMI", "BLOOD_PRESSURE", "BLOOD_GLUCOSE", "FETAL_MOVEMENT_SESSION", "HYDRATION",
            "EPDS_SCORE", "MATERNAL_HEART_RATE", "STRESS");
    private static final String DISCLAIMER = "Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.";

    private final HealthObservationRepository observationRepository;
    private final MetricDefinitionRepository definitionRepository;
    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final MetricObservationValidator validator;

    @Override
    public MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId) {
        HealthObservation observation = observationRepository.findByIdAndStatus(metricId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Metric not found or deleted: " + metricId));
        MotherJourney journey = journeyForObservation(observation);
        requireOwner(journey, callerId, "METRIC-003");
        return toDetailResponse(observation, journey.getId());
    }

    @Override
    @Transactional
    public void deleteMetric(UUID metricId, UUID callerId) {
        HealthObservation observation = observationRepository.findByIdAndStatus(metricId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Metric not found or deleted: " + metricId));
        MotherJourney journey = journeyForObservation(observation);
        requireOwner(journey, callerId, "METRIC-003");
        observation.setRecordStatus(MetricStatus.DELETED);
        observationRepository.save(observation);
        observationRepository.updateStatus(observation.getId(), MetricStatus.DELETED);
        auditService.log(AuditAction.HEALTH_METRIC_DELETED, callerId,
                "HealthObservation", observation.getId().toString(), "deleted");
        eventPublisher.publishEvent(new MaternalHealthMetricDeleted(
                observation.getId(), journey.getId(), callerId, Instant.now()));
    }

    @Override
    @Transactional
    public MetricResponse addMetric(UUID userId, UUID journeyId, AddMetricRequest request) {
        MotherJourney journey = activeOwnedJourney(journeyId, userId, "METRIC-001", "METRIC-002");
        String metricCode = validator.canonicalCode(request.getMetricType());
        MetricDefinition definition = effectiveDefinition(metricCode);
        MetricObservationValidator.NormalizedObservation normalized = validator.normalize(request, definition);

        HealthObservation observation = HealthObservation.builder()
                .careSubjectId(requireCareSubjectId(journey))
                .metricCode(normalized.metricCode())
                .periodStart(normalized.periodStart())
                .periodEnd(normalized.periodEnd())
                .context(new LinkedHashMap<>(normalized.context()))
                .originalUnit(request.getUnit())
                .definitionVersion(normalized.definitionVersion())
                .observationShape(definition.getObservationShape())
                .valueNumeric(normalized.valueNumeric())
                .valueSecondary(normalized.valueSecondary())
                .unit(normalized.unit())
                .measuredAt(normalized.measuredAt())
                .sourceType(normalized.sourceType())
                .note(normalized.note())
                .qualityLabel("UNKNOWN")
                .payload(new LinkedHashMap<>(normalized.context()))
                .subjectType("MOTHER")
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .build();
        observation.getPayload().put("journeyId", journeyId.toString());
        HealthObservation saved = observationRepository.save(observation);

        auditService.log(AuditAction.HEALTH_METRIC_ADDED, userId,
                "HealthObservation", saved.getId().toString(), "added");
        return toMetricResponse(saved, journeyId, null, false, definition.getVersion());
    }

    @Override
    @Transactional
    public MetricResponse updateMetric(UUID userId, UUID journeyId, UUID metricId, UpdateMetricRequest request) {
        MotherJourney journey = activeOwnedJourney(journeyId, userId, "METRIC-010", "METRIC-013");
        HealthObservation observation = observationRepository.findByIdAndCareSubjectIdAndStatus(
                metricId, requireCareSubjectId(journey), MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-011",
                        "Metric not found in this journey: " + metricId));
        if (observation.getCreatedAt() != null
                && Instant.now().isAfter(observation.getCreatedAt().plus(24, ChronoUnit.HOURS))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-012",
                    "Edit window of 24 hours has expired");
        }

        MetricType legacyType = legacyTypeForCanonical(observation.getMetricCode());
        MetricDefinition definition = effectiveDefinition(observation.getMetricCode());
        Map<String, Object> existingContext = observation.getContext() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(observation.getContext());
        MetricObservationValidator.NormalizedObservation normalized = validator.mergeAndNormalize(
                legacyType,
                observation.getValueNumeric(),
                observation.getValueSecondary(),
                observation.getUnit(),
                observation.getMeasuredAt(),
                observation.getSourceType(),
                observation.getNote(),
                existingContext,
                observation.getPeriodStart(),
                observation.getPeriodEnd(),
                request,
                definition);

        observation.setValueNumeric(normalized.valueNumeric());
        observation.setValueSecondary(normalized.valueSecondary());
        observation.setUnit(normalized.unit());
        observation.setMeasuredAt(normalized.measuredAt());
        observation.setNote(normalized.note());
        observation.setContext(new LinkedHashMap<>(normalized.context()));
        observation.setPeriodStart(normalized.periodStart());
        observation.setPeriodEnd(normalized.periodEnd());
        observation.setOriginalUnit(request.getUnit() == null ? observation.getOriginalUnit() : request.getUnit());
        observation.setDefinitionVersion(normalized.definitionVersion());
        observation.setPayload(new LinkedHashMap<>(normalized.context()));
        observation.getPayload().put("journeyId", journeyId.toString());
        HealthObservation saved = observationRepository.save(observation);
        auditService.log(AuditAction.HEALTH_METRIC_UPDATED, userId,
                "HealthObservation", saved.getId().toString(), "updated");
        return toMetricResponse(saved, journeyId, null, false, definition.getVersion());
    }

    @Override
    public List<MetricCapabilityResponse> getCapabilities(UUID journeyId, UUID callerId) {
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-020",
                        "Journey not found: " + journeyId));
        requireOwner(journey, callerId, "METRIC-021");
        return definitionRepository.findAllEffectiveAt(Instant.now()).stream()
                .filter(definition -> definition.getAllowedJourneyStages() == null
                        || definition.getAllowedJourneyStages().isEmpty()
                        || definition.getAllowedJourneyStages().contains(journey.getJourneyType().name()))
                .filter(definition -> definition.isManualEntrySupported())
                .filter(definition -> P0_MANUAL_METRICS.contains(definition.getMetricCode()))
                .map(definition -> MetricCapabilityResponse.builder()
                        .metricCode(definition.getMetricCode())
                        .version(definition.getVersion())
                        .displayName(definition.getDisplayName())
                        .observationShape(definition.getObservationShape())
                        .subjectType(definition.getSubjectType())
                        .manualEntrySupported(definition.isManualEntrySupported())
                        .deviceImportSupported(definition.isDeviceImportSupported())
                        .canonicalUnit(definition.getCanonicalUnit())
                        .acceptedInputUnits(definition.getAcceptedInputUnits())
                        .precisionScale(definition.getPrecisionScale())
                        .requiredContextSchema(definition.getRequiredContextSchema())
                        .qualityPolicy(definition.getQualityPolicy())
                        .allowedJourneyStages(definition.getAllowedJourneyStages())
                        .build())
                .toList();
    }

    @Override
    public MetricTrendResponse getMetricTrend(UUID userId, UUID journeyId, MetricType metricType,
            Instant from, Instant to) {
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-020",
                        "Journey not found: " + journeyId));
        requireTrendAccess(journey, userId);
        String metricCode = validator.canonicalCode(metricType);
        MetricDefinition definition = effectiveDefinition(metricCode);
        List<HealthObservation> observations = observationRepository.findTrend(
                requireCareSubjectId(journey), metricCode, MetricStatus.ACTIVE, from, to);
        List<MetricDataPoint> points = observations.stream().map(this::toDataPoint).toList();
        return MetricTrendResponse.builder()
                .metricType(metricCode)
                .unit(definition.getCanonicalUnit())
                .dataPoints(points)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private MotherJourney activeOwnedJourney(UUID journeyId, UUID userId, String notFoundCode, String forbiddenCode) {
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, notFoundCode,
                        "Journey not found: " + journeyId));
        if (!journey.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, forbiddenCode, "Access denied to journey");
        }
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-003", "Journey is not active");
        }
        return journey;
    }

    private UUID requireCareSubjectId(MotherJourney journey) {
        if (journey.getCareSubjectId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "METRIC-040",
                    "Journey has no care subject");
        }
        return journey.getCareSubjectId();
    }

    private MotherJourney journeyForObservation(HealthObservation observation) {
        Object journeyId = observation.getPayload() == null ? null : observation.getPayload().get("journeyId");
        if (journeyId == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "METRIC-002", "Parent journey not found");
        }
        return journeyRepository.findById(UUID.fromString(journeyId.toString()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-002",
                        "Parent journey not found"));
    }

    private void requireOwner(MotherJourney journey, UUID callerId, String code) {
        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, code, "Access denied to health metric");
        }
    }

    private void requireTrendAccess(MotherJourney journey, UUID userId) {
        // Family members must use the group-scoped projection, which enforces the
        // parent permission and the requested metric's child permission. Allowing
        // RECORDS here would expose every journey metric through this generic API.
        if (!journey.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-021", "Access denied to journey");
        }
    }

    private MetricDefinition effectiveDefinition(String metricCode) {
        if (metricCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-030", "Unsupported metric type");
        }
        return definitionRepository.findByMetricCodeAndActiveTrue(metricCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-030",
                        "Metric is not supported: " + metricCode));
    }

    private MetricType legacyTypeForCanonical(String metricCode) {
        return switch (metricCode) {
            // Existing historical records can still be interpreted, but WEIGHT is not
            // exposed as a capability.
            case "WEIGHT" -> MetricType.WEIGHT;
            case "BMI" -> MetricType.BMI;
            case "BLOOD_PRESSURE" -> MetricType.BLOOD_PRESSURE;
            case "BLOOD_GLUCOSE" -> MetricType.BLOOD_GLUCOSE;
            case "FETAL_MOVEMENT_SESSION" -> MetricType.FETAL_MOVEMENT_SESSION;
            case "HYDRATION" -> MetricType.HYDRATION;
            case "EPDS_SCORE" -> MetricType.EPDS_SCORE;
            case "MATERNAL_HEART_RATE" -> MetricType.MATERNAL_HEART_RATE;
            case "STRESS" -> MetricType.STRESS;
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-030", "Unsupported metric type");
        };
    }

    private MetricDetailResponse toDetailResponse(HealthObservation observation, UUID journeyId) {
        return MetricDetailResponse.builder()
                .id(observation.getId())
                .journeyId(journeyId)
                .metricType(observation.getMetricCode())
                .valueNumeric(observation.getValueNumeric())
                .valueSecondary(observation.getValueSecondary())
                .unit(observation.getUnit())
                .measuredAt(observation.getMeasuredAt())
                .sourceType(observation.getSourceType() == null ? null : observation.getSourceType().name())
                .note(observation.getNote())
                .createdAt(observation.getCreatedAt())
                .context(contextWithoutInternal(observation.getContext()))
                .periodStart(observation.getPeriodStart())
                .periodEnd(observation.getPeriodEnd())
                .qualityLabel(observation.getQualityLabel())
                .disclaimer(DISCLAIMER)
                .definitionVersion(observation.getDefinitionVersion())
                .build();
    }

    private MetricResponse toMetricResponse(HealthObservation observation, UUID journeyId,
            String aiInsight, boolean redFlag, int definitionVersion) {
        return MetricResponse.builder()
                .metricId(observation.getId())
                .journeyId(journeyId)
                .metricType(observation.getMetricCode())
                .valueNumeric(observation.getValueNumeric())
                .valueSecondary(observation.getValueSecondary())
                .unit(observation.getUnit())
                .measuredAt(observation.getMeasuredAt())
                .sourceType(observation.getSourceType() == null ? null : observation.getSourceType().name())
                .note(observation.getNote())
                .createdAt(observation.getCreatedAt())
                .updatedAt(observation.getUpdatedAt())
                .context(contextWithoutInternal(observation.getContext()))
                .periodStart(observation.getPeriodStart())
                .periodEnd(observation.getPeriodEnd())
                .qualityLabel(observation.getQualityLabel())
                .disclaimer(DISCLAIMER)
                .definitionVersion(definitionVersion)
                .aiInsight(aiInsight)
                .redFlagAlert(redFlag)
                .build();
    }

    private MetricDataPoint toDataPoint(HealthObservation observation) {
        return MetricDataPoint.builder()
                .metricId(observation.getId())
                .measuredAt(observation.getMeasuredAt())
                .valueNumeric(observation.getValueNumeric())
                .valueSecondary(observation.getValueSecondary())
                .sourceType(observation.getSourceType() == null ? null : observation.getSourceType().name())
                .note(observation.getNote())
                .context(contextWithoutInternal(observation.getContext()))
                .periodStart(observation.getPeriodStart())
                .periodEnd(observation.getPeriodEnd())
                .qualityLabel(observation.getQualityLabel())
                .build();
    }

    private Map<String, Object> contextWithoutInternal(Map<String, Object> payload) {
        Map<String, Object> result = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        result.remove("journeyId");
        result.remove("periodStart");
        result.remove("periodEnd");
        result.remove("definitionVersion");
        result.remove("metricCode");
        result.remove("originalUnit");
        result.remove("sourceType");
        result.remove("recordStatus");
        return result;
    }

    private Instant instantFromPayload(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null)
            return null;
        if (value instanceof Instant instant)
            return instant;
        return Instant.parse(value.toString());
    }

    private Integer integerFromPayload(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null)
            return null;
        return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
    }
}
