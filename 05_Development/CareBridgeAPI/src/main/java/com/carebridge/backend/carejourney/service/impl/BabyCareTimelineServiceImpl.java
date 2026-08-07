package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.BabyCareTimelineResponse;
import com.carebridge.backend.carejourney.repository.*;
import com.carebridge.backend.carejourney.service.IBabyCareTimelineService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BabyCareTimelineServiceImpl implements IBabyCareTimelineService {
    private final BabyProfileRepository babyProfileRepository;
    private final BabyAccessPolicy babyAccessPolicy;
    private final BabyDailyLogRepository dailyLogs;
    private final GrowthMeasurementStore growth;
    private final DevelopmentMilestoneRepository milestones;
    private final VaccinationRecordRepository vaccinations;

    @Override
    public BabyCareTimelineResponse getTimeline(UUID babyId, String cursor, int size, UUID callerId) {
        var baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found: " + babyId));
        if (!babyAccessPolicy.canView(baby, callerId)) throw new AccessDeniedBusinessException("Baby timeline is not accessible");
        int limit = Math.max(1, Math.min(size, 100));
        var events = Stream.of(
                dailyLogs.findByBabyId(babyId).stream().filter(l -> l.getStatus() == null || l.getStatus().name().equals("ACTIVE"))
                        .map(l -> new BabyCareTimelineResponse.Event("JOURNAL", l.getBabyLogId(), l.getCreatedAt(), l.getLogType())),
                growth.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(babyId).stream()
                        .map(g -> new BabyCareTimelineResponse.Event("GROWTH", g.getGrowthMeasurementId(), g.getMeasuredDate().atStartOfDay().toInstant(ZoneOffset.UTC), "Growth measurement")),
                milestones.findByBabyIdOrderByAchievedDateDesc(babyId).stream().filter(m -> m.getRecordStatus() == null || m.getRecordStatus().name().equals("ACTIVE"))
                        .map(m -> new BabyCareTimelineResponse.Event("MILESTONE", m.getMilestoneId(), Optional.ofNullable(m.getAchievedDate()).orElse(java.time.LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC), m.getMilestoneType())),
                vaccinations.findAllByBabyId(babyId).stream().filter(v -> v.getStatus() == null || !v.getStatus().name().equals("DELETED"))
                        .map(v -> new BabyCareTimelineResponse.Event("VACCINATION", v.getId(), Optional.ofNullable(v.getAdministeredDate()).orElse(v.getScheduledDate()) == null ? Instant.EPOCH : Optional.ofNullable(v.getAdministeredDate()).orElse(v.getScheduledDate()).atStartOfDay().toInstant(ZoneOffset.UTC), v.getVaccineName()))
        ).flatMap(s -> s).sorted(Comparator.comparing(BabyCareTimelineResponse.Event::occurredAt).reversed().thenComparing(e -> e.sourceId().toString())).limit(limit).toList();
        return new BabyCareTimelineResponse(babyId, events, null);
    }
}
