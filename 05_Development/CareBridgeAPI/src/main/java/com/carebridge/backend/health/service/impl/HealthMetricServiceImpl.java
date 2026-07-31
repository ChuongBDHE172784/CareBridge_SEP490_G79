package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.event.MaternalHealthMetricDeleted;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.health.service.IHealthMetricService;
import com.carebridge.backend.health.service.MetricAiAnalyzer;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements IHealthMetricService {

    private final MaternalHealthMetricRepository metricRepository;
    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;
    private final MetricAiAnalyzer metricAiAnalyzer;
    private final ApplicationEventPublisher eventPublisher;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final CareGroupRepository careGroupRepository;

    @Override
    public MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId) {
        // C3: DELETED returns 404
        MaternalHealthMetric metric = metricRepository.findByIdAndStatus(metricId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Metric not found or deleted: " + metricId));

        // C1: ownership via journey — metric → journey → owner_user_id
        MotherJourney journey = journeyRepository.findById(metric.getJourneyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-002",
                        "Parent journey not found for metric: " + metricId));

        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-003",
                    "Access denied to health metric");
        }

        // C2: no diagnosis in response (BR-SAFETY)
        return MetricDetailResponse.builder()
                .id(metric.getId())
                .journeyId(metric.getJourneyId())
                .metricType(metric.getMetricType().name())
                .valueNumeric(metric.getValueNumeric())
                .valueSecondary(metric.getValueSecondary())
                .unit(metric.getUnit())
                .measuredAt(metric.getMeasuredAt())
                .sourceType(metric.getSourceType() != null ? metric.getSourceType().name() : null)
                .note(metric.getNote())
                .createdAt(metric.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteMetric(UUID metricId, UUID callerId) {
        MaternalHealthMetric metric = metricRepository.findByIdAndStatus(metricId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Metric not found or deleted: " + metricId));

        MotherJourney journey = journeyRepository.findById(metric.getJourneyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-002",
                        "Parent journey not found for metric: " + metricId));

        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-003",
                    "Access denied to health metric");
        }

        metric.setStatus(MetricStatus.DELETED);
        metricRepository.save(metric);
        metricRepository.updateStatus(metric.getId(), MetricStatus.DELETED);
        auditService.log(AuditAction.HEALTH_METRIC_DELETED, callerId,
                "MaternalHealthMetric", metric.getId().toString(), "deleted");
        eventPublisher.publishEvent(new MaternalHealthMetricDeleted(
                metric.getId(), metric.getJourneyId(), callerId, Instant.now()));
    }

    @Override
    @Transactional
    public MetricResponse addMetric(UUID userId, UUID journeyId, AddMetricRequest request) {
        // C1: journey must exist
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Journey not found: " + journeyId));

        // C1: ownership check
        if (!journey.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-002",
                    "Access denied to journey");
        }

        // C2: journey must be ACTIVE
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-003",
                    "Journal is not active");
        }

        // C3: measuredAt cannot be more than 5 minutes in the future
        if (request.getMeasuredAt().isAfter(Instant.now().plusSeconds(300))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-004",
                    "measuredAt cannot be more than 5 minutes in the future");
        }

        // C5: blood pressure types require valueSecondary
        if (isBpType(request.getMetricType()) && request.getValueSecondary() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-005",
                    "Blood pressure metrics require both valueNumeric and valueSecondary");
        }

        MaternalHealthMetric metric = MaternalHealthMetric.builder()
                .journeyId(journeyId)
                .careSubjectId(journey.getCareSubjectId())
                .metricType(request.getMetricType())
                .valueNumeric(request.getValueNumeric())
                .valueSecondary(request.getValueSecondary())
                .unit(request.getUnit())
                .measuredAt(request.getMeasuredAt())
                .sourceType(request.getSourceType())
                .note(request.getNote())
                .build();

        MaternalHealthMetric saved = metricRepository.save(metric);

        // C4: async AI — fail-open
        String aiInsight = null;
        boolean redFlag = false;
        if (metricAiAnalyzer != null) {
            try {
                MetricAiAnalyzer.InsightResult insight = metricAiAnalyzer
                        .analyze(saved.getMetricType(), saved.getValueNumeric(), saved.getValueSecondary())
                        .get(5, java.util.concurrent.TimeUnit.SECONDS);
                if (insight != null) {
                    aiInsight = insight.insight();
                    redFlag = insight.redFlag();
                }
            } catch (Exception ignored) {
                // fail-open: metric already saved, AI insight is optional
            }
        }

        auditService.log(AuditAction.HEALTH_METRIC_ADDED, userId,
                "MaternalHealthMetric", saved.getId().toString(), "added");

        return toMetricResponse(saved, aiInsight, redFlag);
    }

    @Override
    @Transactional
    public MetricResponse updateMetric(UUID userId, UUID journeyId, UUID metricId, UpdateMetricRequest request) {
        // C1: journey must exist
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-010",
                        "Journey not found: " + journeyId));

        // C1: ownership check
        if (!journey.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-013",
                    "Access denied to journey");
        }

        // C2: metric must belong to this journey and be ACTIVE
        MaternalHealthMetric metric = metricRepository.findByIdAndJourneyIdAndStatus(
                        metricId, journeyId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-011",
                        "Metric not found in this journey: " + metricId));

        // C3: 24-hour edit window (from created_at, not measured_at)
        if (Instant.now().isAfter(metric.getCreatedAt().plus(24, java.time.temporal.ChronoUnit.HOURS))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "METRIC-012",
                    "Edit window of 24 hours has expired");
        }

        // C4: metricType is immutable — apply only mutable fields
        if (request.getValueNumeric() != null) metric.setValueNumeric(request.getValueNumeric());
        if (request.getValueSecondary() != null) metric.setValueSecondary(request.getValueSecondary());
        if (request.getUnit() != null) metric.setUnit(request.getUnit());
        if (request.getMeasuredAt() != null) metric.setMeasuredAt(request.getMeasuredAt());
        if (request.getNote() != null) metric.setNote(request.getNote());

        MaternalHealthMetric saved = metricRepository.save(metric);

        // C5: audit with old+new snapshot
        auditService.log(AuditAction.HEALTH_METRIC_UPDATED, userId,
                "MaternalHealthMetric", saved.getId().toString(), "updated");

        return toMetricResponse(saved, null, false);
    }

    @Override
    public MetricTrendResponse getMetricTrend(UUID userId, UUID journeyId, MetricType metricType, Instant from, Instant to) {
        // C1: journey must exist
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-020",
                        "Journey not found: " + journeyId));

        // C1: ownership or care group member check
        if (!journey.getOwnerUserId().equals(userId)) {
            boolean isCareGroupMember = false;
            if (careGroupMemberRepository != null && careGroupRepository != null) {
                var memberships = careGroupMemberRepository.findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED);
                for (var member : memberships) {
                    var groupOpt = careGroupRepository.findById(member.getCareGroupId());
                    if (groupOpt.isPresent() && groupOpt.get().getOwnerUserId().equals(journey.getOwnerUserId())) {
                        isCareGroupMember = true;
                        break;
                    }
                }
            }
            if (!isCareGroupMember) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-021",
                        "Access denied to journey");
            }
        }

        // C2: fetch data — sorted ASC by measuredAt; empty list = 200 OK (not 404)
        java.util.List<String> typesToQuery = isBpType(metricType)
                ? java.util.List.of(MetricType.BLOOD_PRESSURE_SYSTOLIC.name(), MetricType.BLOOD_PRESSURE_DIASTOLIC.name())
                : java.util.List.of(metricType.name());

        var metrics = metricRepository
                .findByJourneyIdAndMetricTypeInAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        journeyId, typesToQuery, MetricStatus.ACTIVE, from, to);

        var dataPoints = metrics.stream()
                .map(m -> {
                    java.math.BigDecimal valNum = m.getValueNumeric();
                    java.math.BigDecimal valSec = m.getValueSecondary();
                    if (metricType == MetricType.BLOOD_PRESSURE_DIASTOLIC) {
                        if (m.getMetricType() == MetricType.BLOOD_PRESSURE_SYSTOLIC && valSec != null) {
                            valNum = m.getValueSecondary();
                            valSec = m.getValueNumeric();
                        }
                    } else if (metricType == MetricType.BLOOD_PRESSURE_SYSTOLIC) {
                        if (m.getMetricType() == MetricType.BLOOD_PRESSURE_DIASTOLIC && valSec != null) {
                            valNum = m.getValueSecondary();
                            valSec = m.getValueNumeric();
                        }
                    }
                    return com.carebridge.backend.health.dto.MetricDataPoint.builder()
                            .metricId(m.getId())
                            .measuredAt(m.getMeasuredAt())
                            .valueNumeric(valNum)
                            .valueSecondary(valSec)
                            .sourceType(m.getSourceType() != null ? m.getSourceType().name() : null)
                            .note(m.getNote())
                            .build();
                })
                .toList();

        String unit = metrics.isEmpty() ? null : metrics.get(0).getUnit();

        return MetricTrendResponse.builder()
                .metricType(metricType.name())
                .unit(unit)
                .dataPoints(dataPoints)
                .build();
    }

    private static boolean isBpType(MetricType type) {
        return type == MetricType.BLOOD_PRESSURE_SYSTOLIC || type == MetricType.BLOOD_PRESSURE_DIASTOLIC;
    }

    private MetricResponse toMetricResponse(MaternalHealthMetric m, String aiInsight, boolean redFlag) {
        return MetricResponse.builder()
                .metricId(m.getId())
                .journeyId(m.getJourneyId())
                .metricType(m.getMetricType().name())
                .valueNumeric(m.getValueNumeric())
                .valueSecondary(m.getValueSecondary())
                .unit(m.getUnit())
                .measuredAt(m.getMeasuredAt())
                .sourceType(m.getSourceType() != null ? m.getSourceType().name() : null)
                .note(m.getNote())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .aiInsight(aiInsight)
                .redFlagAlert(redFlag)
                .build();
    }
}
