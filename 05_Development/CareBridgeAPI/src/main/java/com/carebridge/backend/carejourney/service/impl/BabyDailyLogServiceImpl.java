package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogRequest;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.BabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.UpdateBabyDailyLogRequest;
import com.carebridge.backend.carejourney.entity.BabyDailyLog;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.service.IBabyDailyLogService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BabyDailyLogServiceImpl implements IBabyDailyLogService {

    private static final Duration EDIT_WINDOW = Duration.ofHours(24);

    private final BabyDailyLogRepository babyDailyLogRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final AuditService auditService;

    // ── UC34: Add Baby Daily Log ──────────────────────────────────

    @Override
    public AddBabyDailyLogResponse addDailyLog(UUID babyId, AddBabyDailyLogRequest request, UUID userId) {
        BabyProfile baby = findBaby(babyId);
        checkOwnership(baby, userId);
        checkActiveStatus(baby);
        validateFeedingUnit(request);

        // C3: recorded_by = JWT userId — server-side, never from request body (ADR-BABY-008)
        BabyDailyLog log = BabyDailyLog.builder()
                .babyId(babyId)
                .logType(request.getLogType())
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .note(request.getNote())
                .recordedBy(userId)
                .build();

        BabyDailyLog saved = babyDailyLogRepository.save(log);

        // C4: emit BABY_LOG_ADDED audit event after successful save (PDPA)
        auditService.log(AuditAction.BABY_LOG_ADDED, userId,
                "BabyDailyLog", saved.getBabyLogId().toString(), "added");

        return toAddResponse(saved);
    }

    // ── UC35: Update Baby Daily Log ───────────────────────────────

    @Override
    public BabyDailyLogResponse updateLog(UUID babyId, UUID logId,
                                          UpdateBabyDailyLogRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);

        // C1: verify ownership
        BabyProfile baby = findBaby(babyId);
        checkOwnership(baby, userId);

        BabyDailyLog log = findLog(logId);
        validateLogBelongsToBaby(log, babyId);
        // C3: edit window uses created_at (NOT started_at) — ADR-BABY-005-001
        validateEditWindow(log);

        // C4: log_type is immutable — silently ignore if sent (ADR-BABY-005-003)
        // C6: recorded_by must remain original recorder — do NOT update
        if (request.getStartedAt() != null) log.setStartedAt(request.getStartedAt());
        if (request.getEndedAt() != null) log.setEndedAt(request.getEndedAt());
        if (request.getQuantity() != null) log.setQuantity(request.getQuantity());
        if (request.getUnit() != null) log.setUnit(request.getUnit());
        if (request.getNote() != null) log.setNote(request.getNote());

        BabyDailyLog saved = babyDailyLogRepository.save(log);

        // C5: emit audit event
        auditService.log(AuditAction.BABY_LOG_UPDATED, userId,
                "BabyDailyLog", logId.toString(), "updated");

        return toResponse(saved);
    }

    @Override
    public void deleteLog(UUID babyId, UUID logId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);

        // C1: verify ownership
        BabyProfile baby = findBaby(babyId);
        checkOwnership(baby, userId);

        BabyDailyLog log = findLog(logId);
        validateLogBelongsToBaby(log, babyId);
        // C3: edit window uses created_at — ADR-BABY-005-001
        validateEditWindow(log);

        // C5: emit BABY_LOG_DELETED BEFORE delete (with log snapshot for traceability)
        String snapshot = String.format("{logType:%s, babyId:%s}", log.getLogType(), log.getBabyId());
        auditService.log(AuditAction.BABY_LOG_DELETED, userId,
                "BabyDailyLog", logId.toString(), snapshot);

        // C7: hard delete — baby logs are not legal records (ADR-BABY-005-002)
        babyDailyLogRepository.deleteById(logId);
    }

    // ── Private helpers ───────────────────────────────────────────

    private BabyProfile findBaby(UUID babyId) {
        return babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found: " + babyId));
    }

    private BabyDailyLog findLog(UUID logId) {
        return babyDailyLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby daily log not found: " + logId));
    }

    private void checkOwnership(BabyProfile baby, UUID userId) {
        if (!baby.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedBusinessException("You do not own this baby profile");
        }
    }

    private void checkActiveStatus(BabyProfile baby) {
        if (BabyProfileStatus.ARCHIVED.equals(baby.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-032",
                    "Cannot add log for an archived baby");
        }
    }

    private void validateFeedingUnit(AddBabyDailyLogRequest request) {
        if ("FEEDING".equals(request.getLogType())
                && request.getQuantity() != null
                && (request.getUnit() == null || request.getUnit().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-036",
                    "unit is required when quantity is provided for FEEDING log");
        }
    }

    private void validateLogBelongsToBaby(BabyDailyLog log, UUID babyId) {
        if (!babyId.equals(log.getBabyId())) {
            throw new ResourceNotFoundException("Log does not belong to this baby");
        }
    }

    private void validateEditWindow(BabyDailyLog log) {
        // C3: use created_at NOT started_at for 24h edit window — ADR-BABY-005-001
        Instant deadline = log.getCreatedAt().plus(EDIT_WINDOW);
        if (Instant.now().isAfter(deadline)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-042",
                    "Edit window expired. Logs can only be modified within 24 hours of creation.");
        }
    }

    private AddBabyDailyLogResponse toAddResponse(BabyDailyLog log) {
        return AddBabyDailyLogResponse.builder()
                .babyLogId(log.getBabyLogId())
                .babyId(log.getBabyId())
                .logType(log.getLogType())
                .startedAt(log.getStartedAt())
                .endedAt(log.getEndedAt())
                .quantity(log.getQuantity())
                .unit(log.getUnit())
                .note(log.getNote())
                .recordedBy(log.getRecordedBy())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private BabyDailyLogResponse toResponse(BabyDailyLog log) {
        return BabyDailyLogResponse.builder()
                .babyLogId(log.getBabyLogId())
                .babyId(log.getBabyId())
                .logType(log.getLogType())
                .startedAt(log.getStartedAt())
                .endedAt(log.getEndedAt())
                .quantity(log.getQuantity())
                .unit(log.getUnit())
                .note(log.getNote())
                .recordedBy(log.getRecordedBy())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
