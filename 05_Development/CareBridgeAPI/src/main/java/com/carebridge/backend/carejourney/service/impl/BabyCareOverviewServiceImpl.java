package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.BabyCareOverviewResponse;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.DevelopmentMilestoneRepository;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.IBabyCareOverviewService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BabyCareOverviewServiceImpl implements IBabyCareOverviewService {
    private final BabyProfileRepository babyProfileRepository;
    private final BabyAccessPolicy babyAccessPolicy;
    private final BabyDailyLogRepository dailyLogRepository;
    private final GrowthMeasurementRepository growthRepository;
    private final DevelopmentMilestoneRepository milestoneRepository;
    private final VaccinationRecordRepository vaccinationRepository;

    @Override
    public BabyCareOverviewResponse getOverview(UUID babyId, UUID callerId) {
        var baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found: " + babyId));
        if (!babyAccessPolicy.canView(baby, callerId)) {
            throw new AccessDeniedBusinessException("Baby care overview is not accessible");
        }
        return new BabyCareOverviewResponse(
                babyId,
                baby.getNickname(),
                dailyLogRepository.findByBabyId(babyId).stream().filter(log -> log.getStatus() == null || log.getStatus().name().equals("ACTIVE")).count(),
                growthRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId).size(),
                milestoneRepository.findByBabyIdOrderByAchievedDateDesc(babyId).size(),
                vaccinationRepository.findByBabyIdAndStatus(babyId, com.carebridge.backend.vaccination.entity.VaccinationRecordStatus.SCHEDULED).size(),
                "For observation and appointment preparation; not a medical assessment.");
    }
}
