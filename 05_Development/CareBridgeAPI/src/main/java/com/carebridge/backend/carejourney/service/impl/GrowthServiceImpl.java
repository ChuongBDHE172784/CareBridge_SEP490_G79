package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AddGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.dto.GrowthDataPoint;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementHistoryItem;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementResponse;
import com.carebridge.backend.carejourney.dto.UpdateGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.IGrowthService;
import com.carebridge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GrowthServiceImpl implements IGrowthService {

    private final BabyProfileRepository babyProfileRepository;
    private final GrowthMeasurementRepository growthMeasurementRepository;
    private final AuditService auditService;

    @Override
    public GrowthChartResponse getGrowthChart(UUID userId, UUID babyId) {
        BabyProfile baby = getBabyOrThrow(babyId);
        assertOwner(baby, userId);

        List<GrowthMeasurement> measurements =
                growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId);

        List<GrowthDataPoint> dataPoints = measurements.stream()
                .map(m -> GrowthDataPoint.builder()
                        .growthMeasurementId(m.getGrowthMeasurementId())
                        .measuredDate(m.getMeasuredDate())
                        .weightKg(m.getWeightKg())
                        .heightCm(m.getHeightCm())
                        .headCircumferenceCm(m.getHeadCircumferenceCm())
                        .note(m.getNote())
                        .ageInDays(baby.getBirthDate() == null ? 0
                                : (int) ChronoUnit.DAYS.between(baby.getBirthDate(), m.getMeasuredDate()))
                        .build())
                .collect(Collectors.toList());

        return GrowthChartResponse.builder()
                .babyId(baby.getId())
                .nickname(baby.getNickname())
                .birthDate(baby.getBirthDate())
                .measurements(dataPoints)
                .build();
    }

    @Override
    @Transactional
    public GrowthMeasurementResponse addGrowthMeasurement(UUID userId, UUID babyId,
                                                          AddGrowthMeasurementRequest request) {
        BabyProfile baby = getBabyOrThrow(babyId);
        assertOwner(baby, userId);
        assertActive(baby);
        if (!hasAnyMeasurement(request.getWeightKg(), request.getHeightCm(), request.getHeadCircumferenceCm())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-072",
                    "At least one measurement value is required");
        }
        if (hasNegative(request.getWeightKg(), request.getHeightCm(), request.getHeadCircumferenceCm())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-074",
                    "Measurement values must be non-negative");
        }

        GrowthMeasurement measurement = GrowthMeasurement.builder()
                .babyId(babyId)
                .measuredDate(request.getMeasuredDate())
                .weightKg(request.getWeightKg())
                .heightCm(request.getHeightCm())
                .headCircumferenceCm(request.getHeadCircumferenceCm())
                .sourceType(request.getSourceType())
                .note(request.getNote())
                .build();
        GrowthMeasurement saved = growthMeasurementRepository.save(measurement);
        auditService.log(AuditAction.GROWTH_MEASUREMENT_ADDED, userId, "GROWTH_MEASUREMENT",
                saved.getGrowthMeasurementId().toString(), "Added growth measurement for baby " + babyId);
        return toGrowthMeasurementResponse(saved);
    }

    @Override
    @Transactional
    public GrowthMeasurementResponse updateGrowthMeasurement(UUID userId, UUID babyId, UUID growthMeasurementId,
                                                             UpdateGrowthMeasurementRequest request) {
        BabyProfile baby = getBabyOrThrow(babyId);
        assertOwner(baby, userId);
        assertActive(baby);
        if (isEmptyUpdate(request)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-076",
                    "At least one field is required");
        }

        GrowthMeasurement measurement = growthMeasurementRepository.findById(growthMeasurementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-079",
                        "Growth measurement not found"));
        if (!measurement.getBabyId().equals(babyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-079",
                    "Growth measurement does not belong to the baby");
        }
        if (measurement.getDeletedAt() != null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "BABY-079", "Growth measurement not found");
        }

        if (request.getMeasuredDate() != null) {
            measurement.setMeasuredDate(request.getMeasuredDate());
        }
        if (request.getWeightKg() != null) {
            measurement.setWeightKg(request.getWeightKg());
        }
        if (request.getHeightCm() != null) {
            measurement.setHeightCm(request.getHeightCm());
        }
        if (request.getHeadCircumferenceCm() != null) {
            measurement.setHeadCircumferenceCm(request.getHeadCircumferenceCm());
        }
        if (request.getSourceType() != null) {
            measurement.setSourceType(request.getSourceType());
        }
        if (request.getNote() != null) {
            measurement.setNote(request.getNote());
        }

        if (hasNegative(measurement.getWeightKg(), measurement.getHeightCm(), measurement.getHeadCircumferenceCm())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-078",
                    "Measurement values must be non-negative");
        }
        if (!hasAnyMeasurement(measurement.getWeightKg(), measurement.getHeightCm(),
                measurement.getHeadCircumferenceCm())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-077",
                    "At least one measurement value is required");
        }

        GrowthMeasurement saved = growthMeasurementRepository.save(measurement);
        auditService.log(AuditAction.GROWTH_MEASUREMENT_UPDATED, userId, "GROWTH_MEASUREMENT",
                saved.getGrowthMeasurementId().toString(), "Updated growth measurement");
        return toGrowthMeasurementResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGrowthMeasurement(UUID userId, UUID babyId, UUID growthMeasurementId) {
        BabyProfile baby = getBabyOrThrow(babyId);
        assertOwner(baby, userId);
        assertActive(baby);

        GrowthMeasurement measurement = growthMeasurementRepository.findById(growthMeasurementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-080",
                        "Growth measurement not found"));
        if (!measurement.getBabyId().equals(babyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-081",
                    "Growth measurement does not belong to the baby");
        }
        if (measurement.getDeletedAt() != null) {
            return;
        }

        measurement.setDeletedAt(Instant.now());
        growthMeasurementRepository.save(measurement);
        auditService.log(AuditAction.GROWTH_MEASUREMENT_DELETED, userId, "GROWTH_MEASUREMENT",
                measurement.getGrowthMeasurementId().toString(), "Deleted growth measurement");
    }

    @Override
    public Page<GrowthMeasurementHistoryItem> getGrowthMeasurementHistory(UUID userId, UUID babyId, Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 50) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-084",
                    "Invalid growth measurement history pagination");
        }

        BabyProfile baby = getBabyOrThrow(babyId);
        assertOwner(baby, userId);
        return growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(babyId, pageable)
                .map(this::toHistoryItem);
    }

    private BabyProfile getBabyOrThrow(UUID babyId) {
        return babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-070", "Baby not found"));
    }

    private void assertOwner(BabyProfile baby, UUID userId) {
        if (!baby.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-071", "Baby not owned by user");
        }
    }

    private void assertActive(BabyProfile baby) {
        if (baby.getStatus() != BabyProfileStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "BABY-073",
                    "Growth measurements can only be changed for active babies");
        }
    }

    private boolean isEmptyUpdate(UpdateGrowthMeasurementRequest request) {
        return request.getMeasuredDate() == null
                && request.getWeightKg() == null
                && request.getHeightCm() == null
                && request.getHeadCircumferenceCm() == null
                && request.getSourceType() == null
                && request.getNote() == null;
    }

    private boolean hasAnyMeasurement(BigDecimal weightKg, BigDecimal heightCm, BigDecimal headCircumferenceCm) {
        return weightKg != null || heightCm != null || headCircumferenceCm != null;
    }

    private boolean hasNegative(BigDecimal weightKg, BigDecimal heightCm, BigDecimal headCircumferenceCm) {
        return isNegative(weightKg) || isNegative(heightCm) || isNegative(headCircumferenceCm);
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private GrowthMeasurementResponse toGrowthMeasurementResponse(GrowthMeasurement measurement) {
        return GrowthMeasurementResponse.builder()
                .growthMeasurementId(measurement.getGrowthMeasurementId())
                .babyId(measurement.getBabyId())
                .measuredDate(measurement.getMeasuredDate())
                .weightKg(measurement.getWeightKg())
                .heightCm(measurement.getHeightCm())
                .headCircumferenceCm(measurement.getHeadCircumferenceCm())
                .sourceType(measurement.getSourceType())
                .note(measurement.getNote())
                .createdAt(measurement.getCreatedAt())
                .updatedAt(measurement.getUpdatedAt())
                .build();
    }

    private GrowthMeasurementHistoryItem toHistoryItem(GrowthMeasurement measurement) {
        return GrowthMeasurementHistoryItem.builder()
                .growthMeasurementId(measurement.getGrowthMeasurementId())
                .measuredDate(measurement.getMeasuredDate())
                .weightKg(measurement.getWeightKg())
                .heightCm(measurement.getHeightCm())
                .headCircumferenceCm(measurement.getHeadCircumferenceCm())
                .sourceType(measurement.getSourceType())
                .note(measurement.getNote())
                .createdAt(measurement.getCreatedAt())
                .build();
    }
}
