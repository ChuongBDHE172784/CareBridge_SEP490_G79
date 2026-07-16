package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AppointmentPreparationSummaryResponse;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.DevelopmentMilestoneRepository;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.IAppointmentPreparationService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentPreparationServiceImpl implements IAppointmentPreparationService {
    private final BabyProfileRepository babyProfileRepository;
    private final BabyAccessPolicy babyAccessPolicy;
    private final BabyDailyLogRepository dailyLogs;
    private final GrowthMeasurementRepository growth;
    private final DevelopmentMilestoneRepository milestones;
    private final VaccinationRecordRepository vaccinations;

    @Override
    public AppointmentPreparationSummaryResponse getSummary(UUID babyId, UUID callerId) {
        var baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found: " + babyId));
        if (!babyAccessPolicy.canView(baby, callerId)) {
            throw new AccessDeniedBusinessException("Appointment preparation is not accessible");
        }
        List<String> facts = new ArrayList<>();
        facts.add("Baby: " + baby.getNickname());
        facts.add("Journal entries: " + dailyLogs.findByBabyId(babyId).size());
        facts.add("Growth measurements: " + growth.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId).size());
        facts.add("Milestones: " + milestones.findByBabyIdOrderByAchievedDateDesc(babyId).size());
        List<String> dueItems = vaccinations.findByBabyIdAndStatus(babyId, VaccinationRecordStatus.SCHEDULED)
                .stream().map(v -> v.getVaccineName() + " (scheduled)").toList();
        return new AppointmentPreparationSummaryResponse(babyId, List.copyOf(facts), dueItems,
                "For observation and appointment preparation; not a medical assessment.");
    }
}
