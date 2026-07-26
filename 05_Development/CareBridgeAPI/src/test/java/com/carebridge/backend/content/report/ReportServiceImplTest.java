package com.carebridge.backend.content.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.content.dto.request.CreateReportRequest;
import com.carebridge.backend.content.dto.response.CreateReportResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ReportException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.service.ReportService;
import com.carebridge.backend.content.service.ReportServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RPT-TC-014-001..004 (Test-Spec §4, reconciled to the real ContentReport entity and the
 * TDS's RPT- design — see Test-Spec §2 Logic Issues Resolved for why this replaced the
 * original ContentReportService/MOD- draft).
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final UUID REPORTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID TARGET_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000014");

    @Mock private ContentReportRepository contentReportRepository;
    @Mock private CommunityQuestionRepository communityQuestionRepository;
    @Mock private CommunityAnswerRepository communityAnswerRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private CreateReportRequest makeRequest() {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType(ReportTargetType.QUESTION);
        req.setTargetId(TARGET_ID);
        req.setCategory(ReportCategory.HARASSMENT);
        req.setDescription("Nội dung xúc phạm");
        return req;
    }

    private ReportService newService() {
        return new ReportServiceImpl(contentReportRepository, communityQuestionRepository,
                communityAnswerRepository, contentRepository, userRepository, auditService);
    }

    private CommunityQuestion approvedQuestion() {
        return CommunityQuestion.builder().id(TARGET_ID).authorId(UUID.randomUUID())
                .status(QuestionStatus.APPROVED).build();
    }

    // RPT-TC-014-001: happy path
    @Test
    void createReport_validRequest_returnsPendingReportAndEmitsAudit() {
        when(communityQuestionRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.of(approvedQuestion()));
        when(contentReportRepository.countByReporterUserIdAndTargetIdAndCreatedAtAfter(
                eq(REPORTER_ID), eq(TARGET_ID), any())).thenReturn(2);
        when(contentReportRepository.existsByReporterUserIdAndTargetIdAndStatusIn(
                REPORTER_ID, TARGET_ID, java.util.List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW))).thenReturn(false);
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(invocation -> {
            ContentReport saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        CreateReportResponse response = newService().createReport(makeRequest(), REPORTER_ID);

        assertThat(response.getReportId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(captor.capture());
        ContentReport saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(saved.getReporterUserId()).isEqualTo(REPORTER_ID);
        assertThat(saved.getCategory()).isEqualTo("HARASSMENT");
        assertThat(saved.getTargetType()).isEqualTo(ReportTargetType.QUESTION);

        verify(auditService).log(eq(AuditAction.CONTENT_REPORTED), eq(REPORTER_ID),
                eq("ContentReport"), any(), any());
    }

    // RPT-TC-014-002 (was RPT-004 in TDS numbering): target not found
    @Test
    void createReport_targetNotFound_throwsRpt002() {
        when(communityQuestionRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> newService().createReport(makeRequest(), REPORTER_ID))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getCode()).isEqualTo("RPT-002"));

        verify(contentReportRepository, never()).save(any());
    }

    // RPT-TC-014-003: rate limit exceeded (>= 5 in rolling 24h)
    @Test
    void createReport_rateLimitExceeded_throwsRpt003() {
        when(communityQuestionRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.of(approvedQuestion()));
        when(contentReportRepository.countByReporterUserIdAndTargetIdAndCreatedAtAfter(
                eq(REPORTER_ID), eq(TARGET_ID), any())).thenReturn(5);

        assertThatThrownBy(() -> newService().createReport(makeRequest(), REPORTER_ID))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getCode()).isEqualTo("RPT-003"));

        verify(contentReportRepository, never()).save(any());
    }

    // RPT-TC-014-004: duplicate PENDING report on same target
    @Test
    void createReport_duplicatePending_throwsRpt004() {
        when(communityQuestionRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.of(approvedQuestion()));
        when(contentReportRepository.countByReporterUserIdAndTargetIdAndCreatedAtAfter(
                eq(REPORTER_ID), eq(TARGET_ID), any())).thenReturn(0);
        when(contentReportRepository.existsByReporterUserIdAndTargetIdAndStatusIn(
                REPORTER_ID, TARGET_ID, java.util.List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW))).thenReturn(true);

        assertThatThrownBy(() -> newService().createReport(makeRequest(), REPORTER_ID))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getCode()).isEqualTo("RPT-004"));

        verify(contentReportRepository, never()).save(any());
    }

    // Validates target existence per targetType — ANSWER routes to CommunityAnswerRepository
    @Test
    void createReport_answerTarget_validatesAgainstAnswerRepository() {
        CreateReportRequest request = makeRequest();
        request.setTargetType(ReportTargetType.ANSWER);
        UUID questionId = UUID.randomUUID();
        CommunityAnswer answer = CommunityAnswer.builder().id(TARGET_ID).questionId(questionId)
                .status(AnswerStatus.APPROVED).build();
        CommunityQuestion question = CommunityQuestion.builder().id(questionId).authorId(UUID.randomUUID())
                .status(QuestionStatus.APPROVED).build();
        when(communityAnswerRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.of(answer));
        when(communityQuestionRepository.findById(questionId)).thenReturn(java.util.Optional.of(question));
        when(contentReportRepository.countByReporterUserIdAndTargetIdAndCreatedAtAfter(
                eq(REPORTER_ID), eq(TARGET_ID), any())).thenReturn(0);
        when(contentReportRepository.existsByReporterUserIdAndTargetIdAndStatusIn(
                REPORTER_ID, TARGET_ID, java.util.List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW))).thenReturn(false);
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(invocation -> {
            ContentReport saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        CreateReportResponse response = newService().createReport(request, REPORTER_ID);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
        verify(communityAnswerRepository).findById(TARGET_ID);
    }
}
