package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.dto.UpdatePostpartumLogRequest;
import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import com.carebridge.backend.health.event.PostpartumLogDeleted;
import com.carebridge.backend.health.event.PostpartumLogUpdated;
import com.carebridge.backend.health.repository.PostpartumLogRepository;
import com.carebridge.backend.health.service.IPostpartumLogService;
import com.carebridge.backend.health.service.PostpartumAiAnalyzer;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PostpartumLogServiceImpl implements IPostpartumLogService {

    private final PostpartumLogRepository logRepository;
    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;
    private final PostpartumAiAnalyzer postpartumAiAnalyzer;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<PostpartumLogResponse> listLogs(UUID journeyId, UUID callerId) {
        MotherJourney journey = findJourneyOrThrow(journeyId);
        verifyOwner(journey, callerId);

        return logRepository
                .findByJourneyIdAndStatusOrderByLogDateDescCreatedAtDesc(journeyId, PostpartumLogStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostpartumLogResponse getLogDetail(UUID logId, UUID callerId) {
        PostpartumLog log = findActiveLogOrThrow(logId);
        MotherJourney journey = findJourneyOrThrow(log.getJourneyId());
        verifyOwner(journey, callerId);
        return toResponse(log);
    }

    @Override
    public PostpartumLogResponse updateLog(UUID logId, UUID callerId, UpdatePostpartumLogRequest request) {
        PostpartumLog log = findActiveLogOrThrow(logId);
        MotherJourney journey = findJourneyOrThrow(log.getJourneyId());
        verifyOwner(journey, callerId);

        List<String> changedFields = new ArrayList<>();
        if (request.getPainLevel() != null) {
            log.setPainLevel(request.getPainLevel());
            changedFields.add("painLevel");
        }
        if (request.getBleedingLevel() != null) {
            log.setBleedingLevel(request.getBleedingLevel());
            changedFields.add("bleedingLevel");
        }
        if (request.getMoodLevel() != null) {
            log.setMoodLevel(request.getMoodLevel());
            changedFields.add("moodLevel");
        }
        if (request.getSleepHours() != null) {
            log.setSleepHours(request.getSleepHours());
            changedFields.add("sleepHours");
        }
        if (request.getBreastfeedingNote() != null) {
            log.setBreastfeedingNote(request.getBreastfeedingNote());
            changedFields.add("breastfeedingNote");
        }
        if (request.getSymptomNote() != null) {
            log.setSymptomNote(request.getSymptomNote());
            changedFields.add("symptomNote");
        }

        PostpartumLog saved = logRepository.save(log);
        auditService.log(AuditAction.POSTPARTUM_LOG_UPDATED, callerId,
                "PostpartumLog", saved.getId().toString(), String.join(",", changedFields));
        eventPublisher.publishEvent(new PostpartumLogUpdated(
                saved.getId(), saved.getJourneyId(), callerId, List.copyOf(changedFields), Instant.now()));
        return toResponse(saved);
    }

    @Override
    public void deleteLog(UUID logId, UUID callerId) {
        PostpartumLog log = findActiveLogOrThrow(logId);
        MotherJourney journey = findJourneyOrThrow(log.getJourneyId());
        verifyOwner(journey, callerId);

        log.setStatus(PostpartumLogStatus.DELETED);
        PostpartumLog saved = logRepository.save(log);
        auditService.log(AuditAction.POSTPARTUM_LOG_DELETED, callerId,
                "PostpartumLog", saved.getId().toString(), "deleted");
        eventPublisher.publishEvent(new PostpartumLogDeleted(
                saved.getId(), saved.getJourneyId(), saved.getLogDate(), callerId, Instant.now()));
    }

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

        return toResponseBuilder(saved)
                .aiInsight(aiInsight)
                .redFlagAlert(redFlag)
                .build();
    }

    private PostpartumLog findActiveLogOrThrow(UUID logId) {
        return logRepository.findByIdAndStatus(logId, PostpartumLogStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PPLOG-001",
                        "Postpartum log not found or deleted: " + logId));
    }

    private MotherJourney findJourneyOrThrow(UUID journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PPLOG-002",
                        "Parent journey not found"));
    }

    private void verifyOwner(MotherJourney journey, UUID callerId) {
        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "PPLOG-003",
                    "Access denied to postpartum log");
        }
    }

    private PostpartumLogResponse toResponse(PostpartumLog log) {
        return toResponseBuilder(log)
                .aiInsight(null)
                .redFlagAlert(false)
                .build();
    }

    private PostpartumLogResponse.PostpartumLogResponseBuilder toResponseBuilder(PostpartumLog log) {
        return PostpartumLogResponse.builder()
                .postpartumLogId(log.getId())
                .journeyId(log.getJourneyId())
                .logDate(log.getLogDate())
                .painLevel(log.getPainLevel())
                .bleedingLevel(log.getBleedingLevel() != null ? log.getBleedingLevel().name() : null)
                .moodLevel(log.getMoodLevel())
                .sleepHours(log.getSleepHours())
                .breastfeedingNote(log.getBreastfeedingNote())
                .symptomNote(log.getSymptomNote())
                .createdAt(log.getCreatedAt());
    }

}
