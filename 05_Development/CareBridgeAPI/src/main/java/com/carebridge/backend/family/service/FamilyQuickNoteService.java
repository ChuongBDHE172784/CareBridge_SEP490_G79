package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.dto.MetricDataPoint;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only projection of a mother's explicitly shared quick-note history. */
@Service
@Transactional(readOnly = true)
public class FamilyQuickNoteService {

    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final MotherJourneyRepository journeyRepository;
    private final HealthObservationRepository observationRepository;

    public FamilyQuickNoteService(
            CareGroupRepository groupRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            HealthObservationRepository observationRepository) {
        this.groupRepository = groupRepository;
        this.authorizationPolicy = authorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.observationRepository = observationRepository;
    }

    public MetricTrendResponse getHistory(
            UUID careGroupId,
            UUID callerId,
            MetricType metricType,
            Instant from,
            Instant to) {
        CareGroup group = groupRepository.findById(careGroupId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "FAM-005", "Care group not found: " + careGroupId));

        if (group.getStatus() != CareGroupStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FAM-063", "Care group is not active.");
        }

        if (!authorizationPolicy.isMember(careGroupId, callerId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FAM-063", "Care group is not an accepted membership.");
        }
        if (from == null || to == null || from.isAfter(to)
                || java.time.Duration.between(from, to).toDays() > 366) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "FAM-068",
                    "Quick-note history requires a valid range of at most 366 days.");
        }
        requireQuickNotePermission(careGroupId, callerId, metricType);

        var journey = group.getLinkedJourneyId() == null
                ? journeyRepository.findCanonical(group.getOwnerUserId())
                : journeyRepository.findById(group.getLinkedJourneyId())
                        .filter(item -> item.getOwnerUserId().equals(group.getOwnerUserId()));
        if (journey.isEmpty() || journey.get().getCareSubjectId() == null) {
            return empty(metricType);
        }

        String metricCode = canonicalMetricCode(metricType);
        List<HealthObservation> observations = observationRepository.findTrend(
                journey.get().getCareSubjectId(), metricCode, MetricStatus.ACTIVE, from, to);
        List<MetricDataPoint> points = observations.reversed().stream()
                .map(metric -> toReadOnlyPoint(metric, metricType))
                .toList();

        return MetricTrendResponse.builder()
                .metricType(metricType.name())
                .unit(observations.isEmpty() || observations.get(0).getUnit() == null
                        ? defaultUnit(metricType) : observations.get(0).getUnit())
                .dataPoints(points)
                .build();
    }

    private void requireQuickNotePermission(UUID groupId, UUID callerId, MetricType metricType) {
        boolean owner = authorizationPolicy.isOwner(groupId, callerId);
        boolean parentAllowed = owner || authorizationPolicy.hasPermission(
                groupId, callerId, PermissionFlag.QUICK_NOTES);
        PermissionFlag child = switch (metricType) {
            case WEIGHT -> PermissionFlag.QUICK_NOTE_WEIGHT;
            case HYDRATION -> PermissionFlag.QUICK_NOTE_HYDRATION;
            case EPDS_SCORE -> PermissionFlag.QUICK_NOTE_EPDS;
            case FETAL_MOVEMENT_COUNT, FETAL_MOVEMENT_SESSION ->
                    PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT;
            case BLOOD_PRESSURE, BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC ->
                    PermissionFlag.QUICK_NOTE_BLOOD_PRESSURE;
            case BLOOD_GLUCOSE -> PermissionFlag.QUICK_NOTE_BLOOD_GLUCOSE;
            default -> throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "FAM-066",
                    "Only quick-note metric types can be shared through this endpoint.");
        };
        if (!parentAllowed || (!owner && !authorizationPolicy.hasPermission(groupId, callerId, child))) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FAM-067",
                    "This quick-note history has not been shared with the member.");
        }
    }

    private MetricDataPoint toReadOnlyPoint(HealthObservation metric, MetricType metricType) {
        return MetricDataPoint.builder()
                .metricId(metric.getId())
                .measuredAt(metric.getMeasuredAt())
                .valueNumeric(metric.getValueNumeric())
                .valueSecondary(metricType == MetricType.EPDS_SCORE ? null : metric.getValueSecondary())
                .sourceType(metric.getSourceType() == null ? null : metric.getSourceType().name())
                .note(null)
                .context(safeContext(metricType, metric.getContext()))
                .periodStart(isFetalMovement(metricType) ? metric.getPeriodStart() : null)
                .periodEnd(isFetalMovement(metricType) ? metric.getPeriodEnd() : null)
                .build();
    }

    private Map<String, Object> safeContext(MetricType metricType, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        List<String> allowedKeys = switch (metricType) {
            case BLOOD_GLUCOSE -> List.of("measurementContext");
            case FETAL_MOVEMENT_COUNT, FETAL_MOVEMENT_SESSION ->
                    List.of("protocolCode", "completionStatus", "gestationalAgeSnapshot");
            default -> List.of();
        };
        Map<String, Object> sanitized = new LinkedHashMap<>();
        allowedKeys.forEach(key -> {
            Object value = source.get(key);
            if (value != null && isAllowedContextValue(metricType, key, value)) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private boolean isAllowedContextValue(MetricType metricType, String key, Object value) {
        if (metricType != MetricType.BLOOD_GLUCOSE || !"measurementContext".equals(key)) {
            return true;
        }
        return Set.of("FASTING", "PRE_MEAL", "POST_MEAL_1H", "POST_MEAL_2H", "RANDOM", "OTHER_APPROVED")
                .contains(value.toString());
    }

    private boolean isFetalMovement(MetricType metricType) {
        return metricType == MetricType.FETAL_MOVEMENT_COUNT
                || metricType == MetricType.FETAL_MOVEMENT_SESSION;
    }

    private String canonicalMetricCode(MetricType metricType) {
        return switch (metricType) {
            case WEIGHT -> "WEIGHT";
            case HYDRATION -> "HYDRATION";
            case EPDS_SCORE -> "EPDS_SCORE";
            case FETAL_MOVEMENT_COUNT, FETAL_MOVEMENT_SESSION -> "FETAL_MOVEMENT_SESSION";
            case BLOOD_PRESSURE, BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC -> "BLOOD_PRESSURE";
            case BLOOD_GLUCOSE -> "BLOOD_GLUCOSE";
            default -> throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "FAM-066",
                    "Only shared health metric types can be accessed through this endpoint.");
        };
    }

    private MetricTrendResponse empty(MetricType metricType) {
        return MetricTrendResponse.builder()
                .metricType(metricType.name())
                .unit(defaultUnit(metricType))
                .dataPoints(List.of())
                .build();
    }

    private String defaultUnit(MetricType metricType) {
        return switch (metricType) {
            case WEIGHT -> "kg";
            case HYDRATION -> "ml";
            case EPDS_SCORE -> "điểm";
            case FETAL_MOVEMENT_COUNT -> "lần";
            case FETAL_MOVEMENT_SESSION -> "count";
            case BLOOD_PRESSURE, BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC -> "mmHg";
            case BLOOD_GLUCOSE -> "mg/dL";
            default -> "";
        };
    }
}
