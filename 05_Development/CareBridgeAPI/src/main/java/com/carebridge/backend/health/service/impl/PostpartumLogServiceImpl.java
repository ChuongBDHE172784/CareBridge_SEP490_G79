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
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final LifecycleConsentValidator consentValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<PostpartumLogResponse> listLogs(UUID journeyId, UUID callerId, int page, int size) {
        consentValidator.ensureEligibleForRead(callerId);
        MotherJourney journey = requireActivePostpartumOwner(
                journeyId, callerId, false, "POST-001");
        var pageable = PageRequest.of(page, size);
        return logRepository.findByJourneyIdAndStatus(
                journey.getId(), PostpartumLogStatus.ACTIVE, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PostpartumLogResponse getLogDetail(UUID logId, UUID callerId) {
        consentValidator.ensureEligibleForRead(callerId);
        PostpartumLog log = findActiveLogOrThrow(logId);
        requireActivePostpartumOwner(log.getJourneyId(), callerId, false, "PPLOG-001");
        return toResponse(log);
    }

    @Override
    public PostpartumLogResponse updateLog(UUID logId, UUID callerId, UpdatePostpartumLogRequest request) {
        consentValidator.ensureEligibleForMutation(callerId);
        PostpartumLog log = findActiveLogForUpdateOrThrow(logId);
        requireActivePostpartumOwner(log.getJourneyId(), callerId, true, "PPLOG-001");

        List<String> changedFields = new ArrayList<>();
        if (request.getLogDate() != null
                && !Objects.equals(log.getLogDate(), request.getLogDate())) {
            log.setLogDate(request.getLogDate());
            changedFields.add("logDate");
        }
        if (request.getPainLevel() != null
                && !Objects.equals(log.getPainLevel(), request.getPainLevel())) {
            log.setPainLevel(request.getPainLevel());
            changedFields.add("painLevel");
        }
        if (request.getBleedingLevel() != null
                && !Objects.equals(log.getBleedingLevel(), request.getBleedingLevel())) {
            log.setBleedingLevel(request.getBleedingLevel());
            changedFields.add("bleedingLevel");
        }
        if (request.getMoodLevel() != null
                && !Objects.equals(log.getMoodLevel(), request.getMoodLevel())) {
            log.setMoodLevel(request.getMoodLevel());
            changedFields.add("moodLevel");
        }
        if (request.getSleepHours() != null
                && !decimalEquals(log.getSleepHours(), request.getSleepHours())) {
            log.setSleepHours(request.getSleepHours());
            changedFields.add("sleepHours");
        }
        if (request.isBreastfeedingNotePresent()) {
            String normalized = normalizeOptionalNote(request.getBreastfeedingNote());
            if (!Objects.equals(log.getBreastfeedingNote(), normalized)) {
                log.setBreastfeedingNote(normalized);
                changedFields.add("breastfeedingNote");
            }
        }
        if (request.isSymptomNotePresent()) {
            String normalized = normalizeOptionalNote(request.getSymptomNote());
            if (!Objects.equals(log.getSymptomNote(), normalized)) {
                log.setSymptomNote(normalized);
                changedFields.add("symptomNote");
            }
        }
        if (changedFields.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PPLOG-004",
                    "At least one postpartum log field must change");
        }

        PostpartumLog saved = logRepository.save(log);
        logRepository.syncPayload(
                saved.getId(),
                saved.getSubmissionId(),
                saved.getMoodLevel(),
                saved.getBreastfeedingNote(),
                saved.getStatus());
        auditService.log(AuditAction.POSTPARTUM_LOG_UPDATED, callerId,
                "PostpartumLog", saved.getId().toString(), String.join(",", changedFields));
        eventPublisher.publishEvent(new PostpartumLogUpdated(
                saved.getId(), saved.getJourneyId(), callerId, List.copyOf(changedFields), Instant.now()));
        return toResponse(saved);
    }

    @Override
    public void deleteLog(UUID logId, UUID callerId) {
        consentValidator.ensureEligibleForMutation(callerId);
        PostpartumLog log = findActiveLogForUpdateOrThrow(logId);
        requireActivePostpartumOwner(log.getJourneyId(), callerId, true, "PPLOG-001");

        log.setStatus(PostpartumLogStatus.DELETED);
        PostpartumLog saved = logRepository.save(log);
        logRepository.updateStatus(saved.getId(), PostpartumLogStatus.DELETED);
        auditService.log(AuditAction.POSTPARTUM_LOG_DELETED, callerId,
                "PostpartumLog", saved.getId().toString(), "deleted");
        eventPublisher.publishEvent(new PostpartumLogDeleted(
                saved.getId(), saved.getJourneyId(), saved.getLogDate(), callerId, Instant.now()));
    }

    @Override
    public PostpartumLogResponse addLog(UUID userId, UUID journeyId, AddPostpartumLogRequest request) {
        consentValidator.ensureEligibleForMutation(userId);
        requireActivePostpartumOwner(journeyId, userId, true, "POST-001");
        logRepository.acquireJourneyMutationLock(journeyId);
        var existing = logRepository.findByJourneyIdAndSubmissionId(
                journeyId, request.getSubmissionId());
        if (existing.isPresent()) {
            if (existing.get().getStatus() == PostpartumLogStatus.DELETED) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "POSTPARTUM_SUBMISSION_GONE",
                        "Submission id belongs to a deleted postpartum recovery log");
            }
            if (!matches(existing.get(), request)) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "POSTPARTUM_SUBMISSION_CONFLICT",
                        "Submission id was already used with different recovery log data");
            }
            return toResponse(existing.get());
        }

        PostpartumLog log = PostpartumLog.builder()
                .journeyId(journeyId)
                .submissionId(request.getSubmissionId())
                .logDate(request.getLogDate())
                .painLevel(request.getPainLevel())
                .bleedingLevel(request.getBleedingLevel())
                .moodLevel(request.getMoodLevel())
                .sleepHours(request.getSleepHours())
                .breastfeedingNote(normalizeOptionalNote(request.getBreastfeedingNote()))
                .symptomNote(normalizeOptionalNote(request.getSymptomNote()))
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

    private PostpartumLog findActiveLogForUpdateOrThrow(UUID logId) {
        return logRepository.findByIdAndStatusForUpdate(logId, PostpartumLogStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PPLOG-001",
                        "Postpartum log not found"));
    }

    private MotherJourney requireActivePostpartumOwner(
            UUID journeyId, UUID callerId, boolean forUpdate, String notFoundCode) {
        MotherJourney journey = (forUpdate
                ? journeyRepository.findByIdForUpdate(journeyId)
                : journeyRepository.findById(journeyId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, notFoundCode,
                        "Postpartum resource not found"));
        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, notFoundCode,
                    "Postpartum resource not found");
        }
        if (journey.getJourneyType() != JourneyType.POSTPARTUM) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "POST-002",
                    "Postpartum logs require a POSTPARTUM journey type");
        }
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "POST-003",
                    "Postpartum journey is not active");
        }
        return journey;
    }

    private boolean matches(PostpartumLog log, AddPostpartumLogRequest request) {
        return java.util.Objects.equals(log.getLogDate(), request.getLogDate())
                && java.util.Objects.equals(log.getPainLevel(), request.getPainLevel())
                && java.util.Objects.equals(log.getBleedingLevel(), request.getBleedingLevel())
                && java.util.Objects.equals(log.getMoodLevel(), request.getMoodLevel())
                && decimalEquals(log.getSleepHours(), request.getSleepHours())
                && java.util.Objects.equals(log.getBreastfeedingNote(),
                        normalizeOptionalNote(request.getBreastfeedingNote()))
                && java.util.Objects.equals(log.getSymptomNote(),
                        normalizeOptionalNote(request.getSymptomNote()));
    }

    private boolean decimalEquals(java.math.BigDecimal left, java.math.BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private String normalizeOptionalNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
                .submissionId(log.getSubmissionId())
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
