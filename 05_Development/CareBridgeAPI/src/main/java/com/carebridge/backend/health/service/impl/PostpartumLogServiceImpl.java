package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.repository.PostpartumLogRepository;
import com.carebridge.backend.health.service.IPostpartumLogService;
import com.carebridge.backend.health.service.PostpartumAiAnalyzer;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PostpartumLogServiceImpl implements IPostpartumLogService {

    private final PostpartumLogRepository logRepository;
    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;
    private final PostpartumAiAnalyzer postpartumAiAnalyzer;

    @Override
    public PostpartumLogResponse addLog(UUID userId, UUID journeyId, AddPostpartumLogRequest request) {
        // C3: journey must exist
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "POST-001",
                        "Journey not found: " + journeyId));

        // C3: ownership check
        if (!journey.getOwnerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "POST-006",
                    "Access denied to journey");
        }

        // C1: journey type must be POSTPARTUM
        if (journey.getJourneyType() != JourneyType.POSTPARTUM) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "POST-002",
                    "Postpartum logs require a POSTPARTUM journey type");
        }

        // C2: journey must be ACTIVE
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "POST-003",
                    "Journey is not active");
        }

        PostpartumLog log = PostpartumLog.builder()
                .journeyId(journeyId)
                .logDate(request.getLogDate())
                .painLevel(request.getPainLevel())
                .bleedingLevel(request.getBleedingLevel())
                .moodLevel(request.getMoodLevel())
                .sleepHours(request.getSleepHours())
                .breastfeedingNote(request.getBreastfeedingNote())
                .symptomNote(request.getSymptomNote())
                .build();

        PostpartumLog saved = logRepository.save(log);

        // C4: async AI — fail-open (BR-SAFETY: guidance only, never diagnose)
        String aiInsight = null;
        boolean redFlag = false;
        try {
            PostpartumAiAnalyzer.InsightResult insight = postpartumAiAnalyzer
                    .analyze(saved.getPainLevel(), saved.getBleedingLevel(),
                             saved.getMoodLevel(), saved.getSleepHours())
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
            if (insight != null) {
                aiInsight = insight.insight();
                redFlag = insight.redFlag();
            }
        } catch (Exception ignored) {
            // fail-open: log already saved, AI insight is optional
        }

        // C6: audit event
        auditService.log(AuditAction.POSTPARTUM_LOG_ADDED, userId,
                "PostpartumLog", saved.getId().toString(), "added");

        return PostpartumLogResponse.builder()
                .postpartumLogId(saved.getId())
                .journeyId(saved.getJourneyId())
                .logDate(saved.getLogDate())
                .painLevel(saved.getPainLevel())
                .bleedingLevel(saved.getBleedingLevel() != null ? saved.getBleedingLevel().name() : null)
                .moodLevel(saved.getMoodLevel())
                .sleepHours(saved.getSleepHours())
                .breastfeedingNote(saved.getBreastfeedingNote())
                .symptomNote(saved.getSymptomNote())
                .createdAt(saved.getCreatedAt())
                .aiInsight(aiInsight)
                .redFlagAlert(redFlag)
                .build();
    }
}
