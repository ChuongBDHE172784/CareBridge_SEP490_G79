package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.dto.AiVerdictMatch;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.aimoderation.exception.AiVerdictParseException;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.policy.AiModerationPromptBuilder;
import com.carebridge.backend.aimoderation.policy.AiVerdictParser;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.service.AiPolicySetService;
import com.carebridge.backend.aimoderation.service.AiPolicySetService.AiPolicySet;
import com.carebridge.backend.aimoderation.service.AiScanProcessingService;
import com.carebridge.backend.aimoderation.service.AiScanResultRecorder;
import com.carebridge.backend.aimoderation.service.AiScanTargetResolver;
import com.carebridge.backend.aimoderation.service.AiScanTargetResolver.TargetContent;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ModerationCallResult;
import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiScanProcessingServiceTest {

    @Mock
    private AiContentScanJobRepository jobRepository;
    @Mock
    private AiContentAssessmentRepository assessmentRepository;
    @Mock
    private AiScanTargetResolver targetResolver;
    @Mock
    private AiPolicySetService policySetService;
    @Mock
    private AiModerationPromptBuilder promptBuilder;
    @Mock
    private AiVerdictParser verdictParser;
    @Mock
    private AiModerationDecisionPolicy decisionPolicy;
    @Mock
    private GeminiModerationClient geminiModerationClient;
    @Mock
    private AiScanResultRecorder recorder;

    @InjectMocks
    private AiScanProcessingService service;

    private static final String CONTENT = "Nội dung câu hỏi thai kỳ bình thường";
    private static final String HASH = AiContentHasher.sha256Hex(CONTENT);
    private static final String POLICY_SET_HASH = "policyhash";
    private static final String MODEL = "gemini-1.5-flash";

    private AiContentScanJob job;
    private AiPolicySet policySet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 2);
        ReflectionTestUtils.setField(service, "staleProcessingMinutes", 10L);
        job = AiContentScanJob.builder()
                .id(UUID.randomUUID())
                .targetType(ReportTargetType.QUESTION)
                .targetId(UUID.randomUUID())
                .contentHash(HASH)
                .status(AiScanJobStatus.PROCESSING)
                .attemptCount(1)
                .nextAttemptAt(Instant.now())
                .build();
        AiModerationPolicy policy = AiModerationPolicy.builder()
                .policyCode("SPAM_ADVERTISING").name("spam").detectionGuidance("g")
                .violationCategory(AiViolationCategory.SPAM_ADVERTISING)
                .reportCategory(ReportCategory.SPAM)
                .severity(AiPolicySeverity.MEDIUM)
                .applicableTargetTypes("QUESTION,ANSWER,CONTENT")
                .confidenceThreshold(new BigDecimal("0.7"))
                .build();
        policySet = new AiPolicySet(List.of(policy), Map.of("SPAM_ADVERTISING", policy), POLICY_SET_HASH);
    }

    private void givenClaimedJobWithTarget() {
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(targetResolver.resolve(job.getTargetType(), job.getTargetId()))
                .thenReturn(TargetContent.of(CONTENT));
    }

    private void givenPoliciesAndNoExistingAssessment() {
        when(policySetService.activeSnapshotFor(job.getTargetType())).thenReturn(policySet);
        when(geminiModerationClient.model()).thenReturn(MODEL);
        when(assessmentRepository
                .findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
                        job.getTargetType(), job.getTargetId(), HASH, POLICY_SET_HASH, MODEL,
                        AiAssessmentStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(promptBuilder.buildSystemInstruction(any())).thenReturn("system");
        when(promptBuilder.buildUserContent(any(), anyString())).thenReturn("user");
        when(promptBuilder.responseSchema()).thenReturn(Map.of("type", "OBJECT"));
    }

    // Scenario 12/A: SAFE persists the assessment and creates no case
    @Test
    void safeResult_recordsSuccessWithoutCase() {
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        AiVerdict verdict = new AiVerdict(AiClassification.SAFE, null, new BigDecimal("0.9"),
                List.of(), AiRecommendedAction.NO_ACTION, null);
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenReturn(new ModerationCallResult("{}", 120, 10, 5));
        when(verdictParser.parse(anyString(), any(), anyString())).thenReturn(verdict);
        when(decisionPolicy.decide(verdict, policySet.byCode())).thenReturn(CaseDecision.none());

        service.processJob(job.getId());

        verify(recorder).recordSuccess(eq(job), eq(POLICY_SET_HASH), eq(MODEL), eq(verdict),
                eq(CaseDecision.none()), eq(120L), eq(10), eq(5));
    }

    // Scenario 10/B: violation verdict flows into an automated case decision
    @Test
    void spamViolation_recordsSuccessWithCaseDecision() {
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        AiVerdict verdict = new AiVerdict(AiClassification.VIOLATION, AiPolicySeverity.MEDIUM,
                new BigDecimal("0.9"),
                List.of(new AiVerdictMatch(UUID.randomUUID(), "SPAM_ADVERTISING", 1,
                        AiViolationCategory.SPAM_ADVERTISING, AiPolicySeverity.MEDIUM,
                        new BigDecimal("0.85"), List.of(), null)),
                AiRecommendedAction.REVIEW, "spam");
        CaseDecision decision = new CaseDecision(true, CasePriority.NORMAL, ReportCategory.SPAM,
                "SPAM_ADVERTISING");
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenReturn(new ModerationCallResult("{}", 100, null, null));
        when(verdictParser.parse(anyString(), any(), anyString())).thenReturn(verdict);
        when(decisionPolicy.decide(verdict, policySet.byCode())).thenReturn(decision);

        service.processJob(job.getId());

        verify(recorder).recordSuccess(eq(job), eq(POLICY_SET_HASH), eq(MODEL), eq(verdict),
                eq(decision), eq(100L), any(), any());
    }

    // Scenario 14/E: an identical completed assessment short-circuits — Gemini is never called
    @Test
    void duplicateCompletedAssessment_skipsGeminiAndCase() {
        givenClaimedJobWithTarget();
        when(policySetService.activeSnapshotFor(job.getTargetType())).thenReturn(policySet);
        when(geminiModerationClient.model()).thenReturn(MODEL);
        when(assessmentRepository
                .findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
                        any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(AiContentAssessment.builder().id(UUID.randomUUID()).build()));

        service.processJob(job.getId());

        verify(recorder).completeIdempotent(job.getId());
        verify(geminiModerationClient, never()).classify(anyString(), anyString(), any());
        verify(recorder, never()).recordSuccess(any(), any(), any(), any(), any(), eq(0L), any(), any());
    }

    // Scenario 15: content changed after enqueue → stale job skipped (new-hash job handles it)
    @Test
    void staleContent_isSkipped() {
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(targetResolver.resolve(job.getTargetType(), job.getTargetId()))
                .thenReturn(TargetContent.of("nội dung đã bị sửa khác hẳn"));

        service.processJob(job.getId());

        verify(recorder).recordSkip(job.getId(), "STALE_CONTENT");
        verifyNoInteractions(geminiModerationClient);
    }

    // Target deleted before the worker ran → terminal skip, no retry loop
    @Test
    void targetGone_isSkippedWithoutRetry() {
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(targetResolver.resolve(job.getTargetType(), job.getTargetId()))
                .thenReturn(TargetContent.skipped("TARGET_GONE"));

        service.processJob(job.getId());

        verify(recorder).recordSkip(job.getId(), "TARGET_GONE");
        verifyNoInteractions(geminiModerationClient);
    }

    // Scenario 5/D: transient failure below the ceiling → requeue with backoff, never SAFE
    @Test
    void retryableFailure_requeuesWithBackoff() {
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenThrow(new GeminiUnavailableException("Gemini server error (HTTP 503)"));

        service.processJob(job.getId());

        verify(recorder).recordRetry(eq(job.getId()), eq("GEMINI_UNAVAILABLE"), any(Instant.class));
        verify(recorder, never()).recordSuccess(any(), any(), any(), any(), any(), eq(0L), any(), any());
        verify(recorder, never()).recordFailure(any(), any(), any(), anyString());
    }

    // Scenario 13/D: attempts exhausted → FAILED assessment, no violation case, not SAFE
    @Test
    void exhaustedRetries_failsWithoutFabricatingViolation() {
        job.setAttemptCount(2); // == maxAttempts
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenThrow(new GeminiUnavailableException("Gemini connection failure or timeout"));

        service.processJob(job.getId());

        verify(recorder).recordFailure(eq(job), eq(POLICY_SET_HASH), eq(MODEL), eq("GEMINI_UNAVAILABLE"));
    }

    // Scenario 6: configuration error (bad model/key) fails immediately — no retry storm
    @Test
    void configurationError_failsJobImmediately() {
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenThrow(new GeminiConfigurationException("GEMINI_MODEL_INVALID", "model rejected"));

        service.processJob(job.getId());

        verify(recorder).recordFailure(eq(job), eq(POLICY_SET_HASH), eq(MODEL), eq("GEMINI_MODEL_INVALID"));
        verify(recorder, never()).recordRetry(any(), anyString(), any());
    }

    // Scenario 4: malformed model JSON is retried as a failure — never treated as SAFE
    @Test
    void malformedResponse_isRetriedNotSafe() {
        givenClaimedJobWithTarget();
        givenPoliciesAndNoExistingAssessment();
        when(geminiModerationClient.classify(anyString(), anyString(), any()))
                .thenReturn(new ModerationCallResult("not-json", 50, null, null));
        when(verdictParser.parse(anyString(), any(), anyString()))
                .thenThrow(new AiVerdictParseException("invalid"));

        service.processJob(job.getId());

        verify(recorder).recordRetry(eq(job.getId()), eq("GEMINI_RESPONSE_INVALID"), any(Instant.class));
        verify(recorder, never()).recordSuccess(any(), any(), any(), any(), any(), eq(0L), any(), any());
    }

    @Test
    void backoff_isBoundedExponential() {
        assertThat(AiScanProcessingService.backoffFor(1).getSeconds()).isEqualTo(30);
        assertThat(AiScanProcessingService.backoffFor(2).getSeconds()).isEqualTo(60);
        assertThat(AiScanProcessingService.backoffFor(3).getSeconds()).isEqualTo(120);
        assertThat(AiScanProcessingService.backoffFor(50).getSeconds()).isEqualTo(900); // capped
    }
}
