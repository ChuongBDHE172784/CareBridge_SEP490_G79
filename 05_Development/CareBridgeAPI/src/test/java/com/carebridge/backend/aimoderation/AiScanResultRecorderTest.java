package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.service.AiModerationCaseService;
import com.carebridge.backend.aimoderation.service.AiScanResultRecorder;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CB-MOD-IMP-017: matches persist inline in ai_content_assessments.matches_jsonb — one
 * atomic INSERT, no ai_assessment_matches table. Uses a REAL AiModerationMapper so the
 * serialize/deserialize round-trip is exercised, not mocked.
 */
@ExtendWith(MockitoExtension.class)
class AiScanResultRecorderTest {

    @Mock
    private AiContentScanJobRepository jobRepository;
    @Mock
    private AiContentAssessmentRepository assessmentRepository;
    @Mock
    private AiModerationCaseService caseService;
    @Mock
    private AuditService auditService;

    private final AiModerationMapper mapper = new AiModerationMapper(new ObjectMapper());

    private AiScanResultRecorder recorder;

    private AiContentScanJob job;

    @BeforeEach
    void setUp() {
        recorder = new AiScanResultRecorder(jobRepository, assessmentRepository, caseService, mapper,
                auditService);
        job = AiContentScanJob.builder()
                .id(UUID.randomUUID())
                .targetType(ReportTargetType.ANSWER)
                .targetId(UUID.randomUUID())
                .contentHash("hash")
                .status(AiScanJobStatus.PROCESSING)
                .attemptCount(1)
                .nextAttemptAt(Instant.now())
                .build();
        org.mockito.Mockito.lenient().when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    }

    // VII.2: an assessment without matches stores the empty JSON array — never null
    @Test
    void safeSuccess_persistsEmptyMatchesArray_noCase_jobCompleted() {
        when(assessmentRepository.save(any(AiContentAssessment.class))).thenAnswer(inv -> {
            AiContentAssessment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        AiVerdict verdict = new AiVerdict(AiClassification.SAFE, null, new BigDecimal("0.95"),
                List.of(), AiRecommendedAction.NO_ACTION, null);

        recorder.recordSuccess(job, "psh", "gemini-1.5-flash", verdict, CaseDecision.none(), 90, 10, 5);

        verifyNoInteractions(caseService);
        assertThat(job.getStatus()).isEqualTo(AiScanJobStatus.COMPLETED);
        ArgumentCaptor<AiContentAssessment> captor = ArgumentCaptor.forClass(AiContentAssessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AiAssessmentStatus.COMPLETED);
        assertThat(captor.getValue().getMatchesJson()).isEqualTo("[]");
        verifyNoInteractions(auditService);
    }

    // VII.1: assessment + matches persist in ONE save — atomic snapshot, typed round-trip
    @Test
    void violationSuccess_persistsMatchesInline_createsAndLinksCase() {
        UUID caseId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(assessmentRepository.save(any(AiContentAssessment.class))).thenAnswer(inv -> {
            AiContentAssessment a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });
        when(caseService.createOrAttachCase(eq(job.getTargetType()), eq(job.getTargetId()), any(), any(), any()))
                .thenReturn(caseId);
        AiVerdictMatch match = new AiVerdictMatch(policyId, "HARASSMENT_BULLYING", 3,
                AiViolationCategory.HARASSMENT_BULLYING, AiPolicySeverity.HIGH,
                new BigDecimal("0.910"), List.of("đồ vô dụng"), "tấn công cá nhân");
        AiVerdict verdict = new AiVerdict(AiClassification.VIOLATION, AiPolicySeverity.HIGH,
                new BigDecimal("0.9"), List.of(match), AiRecommendedAction.PRIORITY_REVIEW, "harassment");
        CaseDecision decision = new CaseDecision(true, CasePriority.HIGH, ReportCategory.HARASSMENT, "X");

        UUID assessmentId = recorder.recordSuccess(job, "psh", "m", verdict, decision, 100, null, null);

        assertThat(assessmentId).isNotNull();
        ArgumentCaptor<AiContentAssessment> captor = ArgumentCaptor.forClass(AiContentAssessment.class);
        verify(assessmentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AiContentAssessment saved = captor.getAllValues().get(0);
        // typed round-trip: the stored JSON deserializes back to the exact snapshot
        List<AiVerdictMatch> roundTrip = mapper.parseMatches(saved.getMatchesJson());
        assertThat(roundTrip).containsExactly(match);
        verify(caseService).createOrAttachCase(eq(job.getTargetType()), eq(job.getTargetId()),
                eq(decision), eq(assessmentId), eq("harassment"));
        verify(auditService).log(eq(AuditAction.AI_SCAN_COMPLETED), isNull(UUID.class),
                eq("AiContentAssessment"), anyString(), any());
    }

    @Test
    void failure_recordsFailedAssessment_withoutCase() {
        when(assessmentRepository.save(any(AiContentAssessment.class))).thenAnswer(inv -> {
            AiContentAssessment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        recorder.recordFailure(job, "psh", "m", "GEMINI_MODEL_INVALID");

        assertThat(job.getStatus()).isEqualTo(AiScanJobStatus.FAILED);
        ArgumentCaptor<AiContentAssessment> captor = ArgumentCaptor.forClass(AiContentAssessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AiAssessmentStatus.FAILED);
        assertThat(captor.getValue().getClassification()).isNull(); // FAILED is never SAFE/VIOLATION
        verifyNoInteractions(caseService);
        ArgumentCaptor<Object> details = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(eq(AuditAction.AI_SCAN_FAILED), isNull(UUID.class),
                eq("AiContentAssessment"), anyString(), details.capture());
        assertThat(String.valueOf(details.getValue())).contains("GEMINI_MODEL_INVALID");
    }

    @Test
    void retry_requeuesJobWithBackoff_withoutAssessment() {
        Instant nextAttempt = Instant.now().plusSeconds(60);
        recorder.recordRetry(job.getId(), "GEMINI_UNAVAILABLE", nextAttempt);

        assertThat(job.getStatus()).isEqualTo(AiScanJobStatus.QUEUED);
        assertThat(job.getNextAttemptAt()).isEqualTo(nextAttempt);
        assertThat(job.getLastErrorCode()).isEqualTo("GEMINI_UNAVAILABLE");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void skip_marksJobSkippedWithReason() {
        recorder.recordSkip(job.getId(), "TARGET_GONE");
        assertThat(job.getStatus()).isEqualTo(AiScanJobStatus.SKIPPED);
        assertThat(job.getLastErrorCode()).isEqualTo("TARGET_GONE");
    }

    // VII.3/VII.5: the typed read path never leaks raw JSON — corrupt stored matches degrade
    // to an empty typed list (moderator view survives), and null/blank behave the same.
    @Test
    void malformedStoredMatches_degradeToEmptyTypedList() {
        assertThat(mapper.parseMatches("this-is-not-json")).isEmpty();
        assertThat(mapper.parseMatches("{\"not\":\"an array\"}")).isEmpty();
        assertThat(mapper.parseMatches(null)).isEmpty();
        assertThat(mapper.parseMatches("")).isEmpty();
    }
}
