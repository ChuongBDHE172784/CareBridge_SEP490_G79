package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues durable scan jobs. Called from the content lifecycle inside the SAME transaction
 * as the content write, so a committed post always has its job committed with it and content
 * creation never waits on (or fails because of) Gemini. Scope: public community
 * QUESTION/ANSWER and published CONTENT only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiScanEnqueueService {

    private static final Set<ReportTargetType> SCANNABLE_TARGETS =
            EnumSet.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER, ReportTargetType.CONTENT);
    private static final Set<AiScanJobStatus> ACTIVE_STATUSES =
            EnumSet.of(AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);

    private final AiContentScanJobRepository jobRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final AiScanTargetResolver targetResolver;

    /** Lifecycle hook — silently no-ops when the business toggle is off or content is blank. */
    @Transactional
    public void enqueueScan(ReportTargetType targetType, UUID targetId, String text) {
        if (!SCANNABLE_TARGETS.contains(targetType) || text == null || text.isBlank()) {
            return;
        }
        if (!businessToggleEnabled()) {
            return;
        }
        enqueue(targetType, targetId, text, false);
    }

    /** Admin-triggered rescan: bypasses the completed-assessment idempotency skip. */
    @Transactional
    public UUID enqueueRescan(ReportTargetType targetType, UUID targetId) {
        if (!SCANNABLE_TARGETS.contains(targetType)) {
            throw AiModerationException.rescanUnsupportedTarget(String.valueOf(targetType));
        }
        AiScanTargetResolver.TargetContent target = targetResolver.resolve(targetType, targetId);
        if (!target.isPresent()) {
            throw AiModerationException.rescanTargetNotFound(targetId.toString());
        }
        return enqueue(targetType, targetId, target.text(), true);
    }

    private UUID enqueue(ReportTargetType targetType, UUID targetId, String text, boolean force) {
        String contentHash = AiContentHasher.sha256Hex(text);
        if (jobRepository.existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(
                targetType, targetId, contentHash, ACTIVE_STATUSES)) {
            // An identical scan is already queued/processing — collapse the duplicate. For a
            // forced rescan the pending job will evaluate the same content anyway.
            return null;
        }
        AiContentScanJob job = jobRepository.save(AiContentScanJob.builder()
                .targetType(targetType)
                .targetId(targetId)
                .contentHash(contentHash)
                .status(AiScanJobStatus.QUEUED)
                .nextAttemptAt(Instant.now())
                .forceRescan(force)
                .build());
        return job.getId();
    }

    private boolean businessToggleEnabled() {
        return systemConfigurationRepository.findFirstByOrderByCreatedAtAsc()
                .map(SystemConfiguration::isAiModerationEnabled)
                .orElse(true);
    }
}
