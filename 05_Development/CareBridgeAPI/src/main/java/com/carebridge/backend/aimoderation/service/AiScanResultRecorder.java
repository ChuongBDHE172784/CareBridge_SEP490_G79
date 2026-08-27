package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.service.AiModerationOutcomeApplier.TargetLockResult;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional persistence steps of the scan worker, separated from the orchestration so
 * the Gemini HTTP call is never made while holding a database transaction (Hikari pool is
 * small). Each method is one atomic outcome for one job. CB-MOD-IMP-017: matches are stored
 * inline in ai_content_assessments.matches_jsonb — assessment + matches are one INSERT, so
 * the snapshot is atomic by construction. Audit details carry only IDs, codes and hashes —
 * never scanned text and never the API key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiScanResultRecorder {

    private final AiContentScanJobRepository jobRepository;
    private final AiContentAssessmentRepository assessmentRepository;
    private final AiModerationCaseService caseService;
    private final AiModerationMapper mapper;
    private final AuditService auditService;
    private final AiModerationOutcomeApplier outcomeApplier;

    /** Success path: persist assessment (matches inline), then apply the case decision. */
    @Transactional
    public UUID recordSuccess(AiContentScanJob job, String policySetHash, String model, AiVerdict verdict,
            CaseDecision decision, long latencyMs, Integer promptTokens, Integer outputTokens) {
        Instant now = Instant.now();
        TargetLockResult targetLock = outcomeApplier.acquireTargetLock(
                job.getTargetType(), job.getTargetId(), job.getContentHash(), job.isForceRescan());
        if (targetLock != TargetLockResult.READY) {
            jobRepository.findById(job.getId()).ifPresent(entity -> {
                entity.setStatus(AiScanJobStatus.SKIPPED);
                entity.setLastErrorCode(targetLock == TargetLockResult.TARGET_GONE
                        ? "TARGET_GONE"
                        : "STALE_CONTENT");
                entity.setCompletedAt(now);
                entity.setLockedBy(null);
                entity.setLockedAt(null);
                jobRepository.save(entity);
            });
            return null;
        }
        Optional<AiContentAssessment> existing = assessmentRepository
                .findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
                        job.getTargetType(), job.getTargetId(), job.getContentHash(), policySetHash,
                        model, AiAssessmentStatus.COMPLETED);
        if (existing.isPresent()) {
            outcomeApplier.applyCompleted(existing.get());
            completeJob(job.getId(), now);
            return existing.get().getId();
        }
        AiContentAssessment assessment;
        try {
            assessment = assessmentRepository.save(AiContentAssessment.builder()
                    .jobId(job.getId())
                    .targetType(job.getTargetType())
                    .targetId(job.getTargetId())
                    .contentHash(job.getContentHash())
                    .policySetHash(policySetHash)
                    .model(model)
                    .status(AiAssessmentStatus.COMPLETED)
                    .classification(verdict.classification())
                    .overallSeverity(verdict.overallSeverity())
                    .confidence(verdict.confidence())
                    .recommendedAction(verdict.recommendedAction())
                    .explanation(verdict.explanation())
                    .matchesJson(mapper.serializeMatches(verdict.matchedPolicies()))
                    .attemptCount(job.getAttemptCount())
                    .latencyMs(latencyMs)
                    .promptTokens(promptTokens)
                    .outputTokens(outputTokens)
                    .completedAt(now)
                    .build());
            assessmentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Another worker completed the identical assessment first (partial unique index).
            // The winner already applied the case decision — just close this job.
            log.info("Duplicate completed assessment detected for job {}, treating as idempotent success",
                    job.getId());
            completeJob(job.getId(), now);
            return null;
        }

        if (decision.createCase()) {
            UUID caseId = caseService.createOrAttachCase(job.getTargetType(), job.getTargetId(), decision,
                    assessment.getId(), verdict.explanation());
            assessment.setModerationCaseId(caseId);
            assessmentRepository.save(assessment);
        }

        outcomeApplier.applyCompleted(assessment);
        completeJob(job.getId(), now);

        if (verdict.classification() != AiClassification.SAFE) {
            auditService.log(AuditAction.AI_SCAN_COMPLETED, (UUID) null, "AiContentAssessment",
                    assessment.getId().toString(),
                    "targetType=" + job.getTargetType() + " targetId=" + job.getTargetId()
                            + " classification=" + verdict.classification()
                            + " matches=" + verdict.matchedPolicies().size()
                            + " caseCreated=" + decision.createCase());
        }
        return assessment.getId();
    }

    /** Idempotency short-circuit: an identical successful assessment already exists. */
    @Transactional
    public void completeIdempotent(AiContentScanJob job, AiContentAssessment assessment) {
        outcomeApplier.applyCompleted(assessment);
        completeJob(job.getId(), Instant.now());
    }

    /** Transient failure below the attempt ceiling: back off and requeue. */
    @Transactional
    public void recordRetry(UUID jobId, String errorCode, Instant nextAttemptAt) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(AiScanJobStatus.QUEUED);
            job.setLastErrorCode(errorCode);
            job.setNextAttemptAt(nextAttemptAt);
            job.setLockedBy(null);
            job.setLockedAt(null);
            jobRepository.save(job);
        });
    }

    /**
     * Terminal failure (config error or attempts exhausted): job FAILED plus one FAILED
     * assessment row for audit. FAILED is never interpreted as SAFE and creates no case.
     */
    @Transactional
    public void recordFailure(AiContentScanJob job, String policySetHash, String model, String errorCode) {
        Instant now = Instant.now();
        jobRepository.findById(job.getId()).ifPresent(entity -> {
            entity.setStatus(AiScanJobStatus.FAILED);
            entity.setLastErrorCode(errorCode);
            entity.setCompletedAt(now);
            entity.setLockedBy(null);
            entity.setLockedAt(null);
            jobRepository.save(entity);
        });
        AiContentAssessment failed = assessmentRepository.save(AiContentAssessment.builder()
                .jobId(job.getId())
                .targetType(job.getTargetType())
                .targetId(job.getTargetId())
                .contentHash(job.getContentHash())
                .policySetHash(policySetHash != null ? policySetHash : "UNKNOWN")
                .model(model)
                .status(AiAssessmentStatus.FAILED)
                .errorCode(errorCode)
                .attemptCount(job.getAttemptCount())
                .completedAt(now)
                .build());
        outcomeApplier.applyHumanReview(job.getTargetType(), job.getTargetId(), job.getContentHash());
        auditService.log(AuditAction.AI_SCAN_FAILED, (UUID) null, "AiContentAssessment",
                failed.getId().toString(),
                "targetType=" + job.getTargetType() + " targetId=" + job.getTargetId()
                        + " errorCode=" + errorCode + " attempts=" + job.getAttemptCount());
    }

    /** Target gone / stale content / no applicable policies: terminal, not an error. */
    @Transactional
    public void recordSkip(UUID jobId, String reason) {
        Instant now = Instant.now();
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(AiScanJobStatus.SKIPPED);
            job.setLastErrorCode(reason);
            job.setCompletedAt(now);
            job.setLockedBy(null);
            job.setLockedAt(null);
            jobRepository.save(job);
            outcomeApplier.applyHumanReview(job.getTargetType(), job.getTargetId(), job.getContentHash());
        });
    }

    private void completeJob(UUID jobId, Instant now) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(AiScanJobStatus.COMPLETED);
            job.setCompletedAt(now);
            job.setLockedBy(null);
            job.setLockedAt(null);
            jobRepository.save(job);
        });
    }
}
