package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.dto.MetricDataPoint;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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
    private final MaternalHealthMetricRepository metricRepository;

    public FamilyQuickNoteService(
            CareGroupRepository groupRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            MaternalHealthMetricRepository metricRepository) {
        this.groupRepository = groupRepository;
        this.authorizationPolicy = authorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.metricRepository = metricRepository;
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
        if (journey.isEmpty()) {
            return empty(metricType);
        }

        List<MaternalHealthMetric> metrics = metricRepository
                .findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        journey.get().getId(), metricType, MetricStatus.ACTIVE, from, to);
        List<MetricDataPoint> points = metrics.stream()
                .sorted(Comparator.comparing(
                        MaternalHealthMetric::getMeasuredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(metric -> toReadOnlyPoint(metric, metricType))
                .toList();

        return MetricTrendResponse.builder()
                .metricType(metricType.name())
                .unit(metrics.isEmpty() ? defaultUnit(metricType) : metrics.get(0).getUnit())
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
            case FETAL_MOVEMENT_COUNT -> PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT;
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

    private MetricDataPoint toReadOnlyPoint(MaternalHealthMetric metric, MetricType metricType) {
        // Only fetal movement uses the note as the event type. Free-text notes and
        // EPDS answer payloads remain private on this list-only family projection.
        String visibleNote = metricType == MetricType.FETAL_MOVEMENT_COUNT ? metric.getNote() : null;
        return MetricDataPoint.builder()
                .metricId(metric.getId())
                .measuredAt(metric.getMeasuredAt())
                .valueNumeric(metric.getValueNumeric())
                .valueSecondary(metric.getValueSecondary())
                .sourceType(metric.getSourceType() == null ? null : metric.getSourceType().name())
                .note(visibleNote)
                .build();
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
            default -> "";
        };
    }
}
