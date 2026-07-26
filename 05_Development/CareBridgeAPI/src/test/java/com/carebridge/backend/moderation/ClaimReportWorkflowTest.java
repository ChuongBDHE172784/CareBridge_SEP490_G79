package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.ResolutionOutcome;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.request.RevertReportRequest;
import com.carebridge.backend.content.dto.response.ClaimReportResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import com.carebridge.backend.expert.handler.IExpertEventHandler;
import com.carebridge.backend.security.repository.UserRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CB-MOD-IMP-016: claim/release atomicity + IN_REVIEW transition guards (scenarios 17/18). */
@ExtendWith(MockitoExtension.class)
class ClaimReportWorkflowTest {

    @Mock
    private ContentReportRepository contentReportRepository;
    @Mock
    private ContentPreviewService contentPreviewService;
    @Mock
    private ModerationMapper moderationMapper;
    @Mock
    private AuditService auditService;
    @Mock
    private CommunityQuestionRepository communityQuestionRepository;
    @Mock
    private CommunityAnswerRepository communityAnswerRepository;
    @Mock
    private ModerationActionRepository moderationActionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IExpertEventHandler expertEventHandler;

    @InjectMocks
    private ModerationServiceImpl service;

    private static final UUID MODERATOR_ID = UUID.randomUUID();
    private static final UUID OTHER_MODERATOR_ID = UUID.randomUUID();
    private static final UUID REPORT_ID = UUID.randomUUID();

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = () -> MODERATOR_ID.toString();
    }

    private ContentReport report(ReportStatus status, UUID assignedModerator) {
        return ContentReport.builder()
                .id(REPORT_ID)
                .targetId(UUID.randomUUID())
                .targetType(ReportTargetType.QUESTION)
                .status(status)
                .assignedModeratorId(assignedModerator)
                .createdAt(Instant.now())
                .build();
    }

    // Scenario 17: guarded UPDATE wins → IN_REVIEW with claimer + timestamp, audited
    @Test
    void claim_pendingReport_succeedsAtomically() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.PENDING, null)));
        when(contentReportRepository.claimReport(eq(REPORT_ID), eq(MODERATOR_ID), any(),
                eq(ReportStatus.PENDING), eq(ReportStatus.IN_REVIEW))).thenReturn(1);

        ClaimReportResponse response = service.claimReport(REPORT_ID, principal);

        assertThat(response.status()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(response.assignedModeratorId()).isEqualTo(MODERATOR_ID);
        assertThat(response.claimedAt()).isNotNull();
        verify(auditService).log(eq(AuditAction.REPORT_CLAIMED), eq(MODERATOR_ID), anyString(),
                eq(REPORT_ID.toString()), any());
    }

    // Scenario 17: the loser of a concurrent claim gets MOD-036 (guarded UPDATE returned 0)
    @Test
    void claim_alreadyClaimed_throwsConflict() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.IN_REVIEW, OTHER_MODERATOR_ID)));
        when(contentReportRepository.claimReport(eq(REPORT_ID), eq(MODERATOR_ID), any(),
                eq(ReportStatus.PENDING), eq(ReportStatus.IN_REVIEW))).thenReturn(0);

        assertThatThrownBy(() -> service.claimReport(REPORT_ID, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-036");
    }

    // Scenario 18: claiming a resolved report is an invalid transition
    @Test
    void claim_resolvedReport_throwsConflict() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.RESOLVED, null)));
        when(contentReportRepository.claimReport(any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.claimReport(REPORT_ID, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-036");
    }

    @Test
    void release_byClaimer_returnsToPending() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.IN_REVIEW, MODERATOR_ID)));
        when(contentReportRepository.releaseReport(eq(REPORT_ID), eq(MODERATOR_ID), any(),
                eq(ReportStatus.PENDING), eq(ReportStatus.IN_REVIEW))).thenReturn(1);

        ClaimReportResponse response = service.releaseReport(REPORT_ID, principal);

        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.assignedModeratorId()).isNull();
        verify(auditService).log(eq(AuditAction.REPORT_RELEASED), eq(MODERATOR_ID), anyString(),
                eq(REPORT_ID.toString()), any());
    }

    // Scenario 18: only the claimer can release
    @Test
    void release_byNonClaimer_throwsDenied() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.IN_REVIEW, OTHER_MODERATOR_ID)));
        when(contentReportRepository.releaseReport(eq(REPORT_ID), eq(MODERATOR_ID), any(),
                eq(ReportStatus.PENDING), eq(ReportStatus.IN_REVIEW))).thenReturn(0);

        assertThatThrownBy(() -> service.releaseReport(REPORT_ID, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-037");
    }

    // Scenario 18: resolving a report that another moderator is reviewing is rejected
    @Test
    void resolve_inReviewByAnotherModerator_throwsConflict() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.IN_REVIEW, OTHER_MODERATOR_ID)));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, null, null);
        assertThatThrownBy(() -> service.resolveReport(REPORT_ID, request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-038");
    }

    // Scenario 18: the claimer can resolve their IN_REVIEW report (DISMISS path)
    @Test
    void resolve_inReviewByClaimer_succeeds() {
        ContentReport claimed = report(ReportStatus.IN_REVIEW, MODERATOR_ID);
        when(contentReportRepository.findById(REPORT_ID)).thenReturn(Optional.of(claimed));
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resolveReport(REPORT_ID, new ResolveReportRequest(ResolutionOutcome.DISMISS, null, null),
                principal);

        assertThat(claimed.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    // Scenario 18: IN_REVIEW is still open — revert is rejected as "not yet resolved"
    @Test
    void revert_inReviewReport_isRejected() {
        when(contentReportRepository.findById(REPORT_ID))
                .thenReturn(Optional.of(report(ReportStatus.IN_REVIEW, MODERATOR_ID)));

        assertThatThrownBy(() -> service.revertReport(REPORT_ID, new RevertReportRequest(null), principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-032");
    }
}
