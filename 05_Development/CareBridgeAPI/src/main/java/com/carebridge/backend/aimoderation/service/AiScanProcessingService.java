package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
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
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ModerationCallResult;
import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        return claimJobs(workerId, now, jobRepository.findClaimableIds(
                AiScanJobStatus.QUEUED, now, PageRequest.of(0, batchSize)));
    }

    /** Claims only pre-publication community jobs for fail-closed human routing. */
    @Transactional
    public List<UUID> claimDueCommunityJobs(String workerId, int batchSize) {
        Instant now = Instant.now();
        jobRepository.requeueStale(now.minus(Duration.ofMinutes(staleProcessingMinutes)), now,
                AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);

        return claimJobs(workerId, now, jobRepository.findClaimableIdsByTargetTypeIn(
                AiScanJobStatus.QUEUED,
                List.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER),
                now,
                PageRequest.of(0, batchSize)));
    }

    private List<UUID> claimJobs(String workerId, Instant now, List<UUID> claimableIds) {
        List<UUID> claimed = new ArrayList<>();
        for (UUID jobId : claimableIds) {
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

    /** Fail-closed drain used when a queued job can no longer be processed by AI. */
    @Async
    public void routeClaimedJobToHumanAsync(UUID jobId, String reason) {
        try {
            AiContentScanJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getStatus() == AiScanJobStatus.PROCESSING) {
                recorder.recordSkip(jobId, reason);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to route AI scan job {} to human review reason={}",
                    jobId, ex.getClass().getSimpleName());
        }
    }

    /**
     * [HÀM CỐT LÕI]: Thực hiện quét kiểm duyệt nội dung bằng AI (Gemini) và so sánh với chính sách hệ thống.
     *
     * Quy trình 7 bước:
     * 1. Trích xuất nội dung cần quét (tiêu đề + nội dung câu hỏi/câu trả lời).
     * 2. Kiểm tra tính hợp lệ và mã băm SHA-256 (bỏ qua nếu nội dung đã bị sửa).
     * 3. Lấy tập hợp chính sách kiểm duyệt hệ thống đang kích hoạt (active policies snapshot).
     * 4. Kiểm tra cache đánh giá trước đó (idempotency check).
     * 5. Gửi nội dung kèm toàn bộ chính sách sang Gemini API (Structured Output).
     * 6. Phân tích kết quả (parse verdict JSON) và đưa ra quyết định dựa trên ngưỡng tin cậy.
     * 7. Ghi nhận kết quả đánh giá (AiContentAssessment) và gọi OutcomeApplier để duyệt hoặc mở case vi phạm.
     *
     * @param jobId UUID của tác vụ quét trong bảng ai_content_scan_jobs
     */
    public void processJob(UUID jobId) {
        // [Bước 1]: Lấy thông tin job quét từ cơ sở dữ liệu
        AiContentScanJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AiScanJobStatus.PROCESSING) {
            return;
        }

        // [Bước 2]: Trích xuất nội dung thực tế từ bảng đích (CommunityQuestion / CommunityAnswer / Content)
        AiScanTargetResolver.TargetContent target = targetResolver.resolve(job.getTargetType(), job.getTargetId());
        if (!target.isPresent()) {
            recorder.recordSkip(job.getId(), target.skipReason());
            return;
        }

        // [Bước 3]: Kiểm tra tính toàn vẹn mã băm (Content Hash). Nếu người dùng đã sửa bài, bỏ qua job cũ này
        String currentHash = AiContentHasher.sha256Hex(target.text());
        if (!currentHash.equals(job.getContentHash())) {
            recorder.recordSkip(job.getId(), "STALE_CONTENT");
            return;
        }

        // [Bước 4]: Lấy danh sách các chính sách kiểm duyệt hệ thống đang có hiệu lực cho loại nội dung này
        AiPolicySet policySet = policySetService.activeSnapshotFor(job.getTargetType());
        if (policySet.isEmpty()) {
            recorder.recordSkip(job.getId(), "NO_ACTIVE_POLICIES");
            return;
        }

        // [Bước 5]: Kiểm tra xem cùng nội dung + cùng bộ chính sách + cùng model đã từng được đánh giá COMPLETED chưa
        String model = geminiModerationClient.model();
        Optional<AiContentAssessment> existingAssessment = !job.isForceRescan() ? assessmentRepository
                .findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
                        job.getTargetType(), job.getTargetId(), job.getContentHash(),
                        policySet.policySetHash(), model, AiAssessmentStatus.COMPLETED)
                : Optional.empty();
        if (existingAssessment.isPresent()) {
            // Tận dụng kết quả đánh giá có sẵn mà không cần gọi lại Gemini API (tiết kiệm chi phí & thời gian)
            recorder.completeIdempotent(job, existingAssessment.get());
            return;
        }

        // [Bước 6]: Gọi Google Gemini API để AI so sánh nội dung bài đăng với danh sách các chính sách
        ModerationCallResult callResult;
        try {
            callResult = geminiModerationClient.classify(
                    promptBuilder.buildSystemInstruction(policySet.policies()), // Nạp danh sách chính sách vào prompt
                    promptBuilder.buildUserContent(job.getTargetType(), target.text()), // Nội dung người dùng cần quét
                    promptBuilder.responseSchema()); // Định dạng JSON Schema đầu ra bắt buộc
        } catch (GeminiConfigurationException ex) {
            // Lỗi cấu hình hoặc vi phạm bộ lọc an toàn của Google Provider (không thử lại)
            recorder.recordFailure(job, policySet.policySetHash(), model, ex.getErrorCode());
            return;
        } catch (GeminiUnavailableException ex) {
            // Lỗi tạm thời (Timeout, mạng, 429) -> Thử lại có giãn cách (Exponential Backoff)
            retryOrFail(job, policySet.policySetHash(), model, "GEMINI_UNAVAILABLE");
            return;
        }

        // [Bước 7.1]: Phân tích kết quả JSON trả về từ Gemini
        AiVerdict verdict;
        try {
            verdict = verdictParser.parse(callResult.rawJson(), policySet.byCode(), target.text());
        } catch (AiVerdictParseException ex) {
            // Nếu model trả về sai JSON Schema, không được mặc định là SAFE mà phải đưa vào xử lý lỗi/thử lại
            retryOrFail(job, policySet.policySetHash(), model, "GEMINI_RESPONSE_INVALID");
            return;
        }

        // [Bước 7.2]: Đưa ra quyết định (CaseDecision) dựa trên mức độ nghiêm trọng và ngưỡng tin cậy của chính sách vi phạm
        CaseDecision decision = decisionPolicy.decide(verdict, policySet.byCode());

        // [Bước 7.3]: Lưu kết quả đánh giá (AiContentAssessment) và tự động duyệt bài hoặc mở ModerationCase
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
