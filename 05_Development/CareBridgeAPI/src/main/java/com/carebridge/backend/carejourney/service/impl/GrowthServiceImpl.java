package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.dto.GrowthDataPoint;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.IGrowthService;
import com.carebridge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public GrowthChartResponse getGrowthChart(UUID userId, UUID babyId) {
        // C1: find baby, throw 404 if not found
        BabyProfile baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-070", "Baby not found"));

        // C1: ownership check, throw 403 if not owner
        if (!baby.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-071", "Baby not owned by user");
        }

        // C2: no status gate — ARCHIVED baby allowed per ADR-BABY-008-002
        List<GrowthMeasurement> measurements =
                growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(babyId);

        // C4: ageInDays calculated in service, not in DB
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
}
