package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.exception.AiVerdictParseException;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.policy.AiModerationPromptBuilder;
import com.carebridge.backend.aimoderation.policy.AiVerdictParser;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.service.AiPolicySetService.AiPolicySet;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ModerationCallResult;
import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scan-job orchestration: claim → resolve target → Gemini → record. The Gemini HTTP call is
 * made outside any database transaction; persistence happens in {@link AiScanResultRecorder}
 * transactional steps. processJob is dispatched via @Async so the (single-threaded) shared
 * scheduler is never blocked for the duration of an HTTP call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiScanProcessingService {

    private static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(15);

    private final AiContentScanJobRepository jobRepository;
    private final AiContentAssessmentRepository assessmentRepository;
    private final AiScanTargetResolver targetResolver;
    private final AiPolicySetService policySetService;
    private final AiModerationPromptBuilder promptBuilder;
    private final AiVerdictParser verdictParser;
    private final AiModerationDecisionPolicy decisionPolicy;
    private final GeminiModerationClient geminiModerationClient;
    private final AiScanResultRecorder recorder;

    @Value("${carebridge.gemini.moderation.max-attempts:4}")
    private int maxAttempts;

    @Value("${carebridge.gemini.moderation.stale-processing-minutes:10}")
    private long staleProcessingMinutes;

    /**
     * Claims up to batchSize due jobs atomically (status-guarded UPDATE; losers of a race
     * simply claim 0 rows). Also requeues PROCESSING jobs whose worker died mid-flight.
     */
    @Transactional
    public List<UUID> claimDueJobs(String workerId, int batchSize) {
        Instant now = Instant.now();
        jobRepository.requeueStale(now.minus(Duration.ofMinutes(staleProcessingMinutes)), now,
                AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);

        List<UUID> claimed = new ArrayList<>();
        for (UUID jobId : jobRepository.findClaimableIds(AiScanJobStatus.QUEUED, now, PageRequest.of(0, batchSize))) {
            if (jobRepository.claim(jobId, workerId, now, AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING) == 1) {
                claimed.add(jobId);
            }
        }
        return claimed;
    }

    @Async
    public void processJobAsync(UUID jobId) {
        try {
            processJob(jobId);
        } catch (RuntimeException ex) {
            // Last-resort guard: an unexpected error must not leave the job stuck in
            // PROCESSING until the stale sweep. Count it as a retryable attempt.
            log.warn("Unexpected AI scan failure for job {} reason={}", jobId, ex.getClass().getSimpleName());
            AiContentScanJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getStatus() == AiScanJobStatus.PROCESSING) {
                retryOrFail(job, null, geminiModerationClient.model(), "SCAN_UNEXPECTED_ERROR");
            }
        }
    }

    /** Synchronous core — also invoked directly by tests. */
    public void processJob(UUID jobId) {
        AiContentScanJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AiScanJobStatus.PROCESSING) {
            return;
        }

        AiScanTargetResolver.TargetContent target = targetResolver.resolve(job.getTargetType(), job.getTargetId());
        if (!target.isPresent()) {
            recorder.recordSkip(job.getId(), target.skipReason());
            return;
        }

        String currentHash = AiContentHasher.sha256Hex(target.text());
        if (!currentHash.equals(job.getContentHash())) {
            // Content changed after enqueue; the edit hook enqueued a fresh job for the new hash.
            recorder.recordSkip(job.getId(), "STALE_CONTENT");
            return;
        }

        AiPolicySet policySet = policySetService.activeSnapshotFor(job.getTargetType());
        if (policySet.isEmpty()) {
            recorder.recordSkip(job.getId(), "NO_ACTIVE_POLICIES");
            return;
        }

        String model = geminiModerationClient.model();
        if (!job.isForceRescan() && assessmentRepository
                .findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
                        job.getTargetType(), job.getTargetId(), job.getContentHash(),
                        policySet.policySetHash(), model, AiAssessmentStatus.COMPLETED)
                .isPresent()) {
            recorder.completeIdempotent(job.getId());
            return;
        }

        ModerationCallResult callResult;
        try {
            callResult = geminiModerationClient.classify(
                    promptBuilder.buildSystemInstruction(policySet.policies()),
                    promptBuilder.buildUserContent(job.getTargetType(), target.text()),
                    promptBuilder.responseSchema());
        } catch (GeminiConfigurationException ex) {
            // Non-retryable: bad key/model/request or provider safety block
            recorder.recordFailure(job, policySet.policySetHash(), model, ex.getErrorCode());
            return;
        } catch (GeminiUnavailableException ex) {
            retryOrFail(job, policySet.policySetHash(), model, "GEMINI_UNAVAILABLE");
            return;
        }

        AiVerdict verdict;
        try {
            verdict = verdictParser.parse(callResult.rawJson(), policySet.byCode(), target.text());
        } catch (AiVerdictParseException ex) {
            // Schema-invalid output is a failure, never SAFE
            retryOrFail(job, policySet.policySetHash(), model, "GEMINI_RESPONSE_INVALID");
            return;
        }

        CaseDecision decision = decisionPolicy.decide(verdict, policySet.byCode());
        recorder.recordSuccess(job, policySet.policySetHash(), model, verdict, decision,
                callResult.latencyMs(), callResult.promptTokens(), callResult.outputTokens());
    }

    private void retryOrFail(AiContentScanJob job, String policySetHash, String model, String errorCode) {
        if (job.getAttemptCount() >= maxAttempts) {
            recorder.recordFailure(job, policySetHash, model, errorCode);
        } else {
            recorder.recordRetry(job.getId(), errorCode, Instant.now().plus(backoffFor(job.getAttemptCount())));
        }
    }

    public static Duration backoffFor(int attemptCount) {
        long multiplier = 1L << Math.min(Math.max(attemptCount - 1, 0), 5);
        Duration backoff = BASE_BACKOFF.multipliedBy(multiplier);
        return backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff;
    }
}
