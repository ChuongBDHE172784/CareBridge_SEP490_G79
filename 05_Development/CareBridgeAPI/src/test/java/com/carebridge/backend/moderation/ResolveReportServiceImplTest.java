package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.ResolutionOutcome;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

// RES-TC-101..118
@ExtendWith(MockitoExtension.class)
class ResolveReportServiceImplTest {

    @Mock
    private ContentReportRepository contentReportRepository;

    @Mock
    private ContentPreviewService contentPreviewService;

    @Spy
    private ModerationMapper moderationMapper = new ModerationMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private CommunityQuestionRepository communityQuestionRepository;

    @Mock
    private CommunityAnswerRepository communityAnswerRepository;

    @Mock
    private ModerationActionRepository moderationActionRepository;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    // === RES-TC Props Isolation Pattern (Test-Spec §4) ===
    private static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID REPORT_ID_QUESTION = UUID.fromString("ee000000-0000-0000-0000-000000000001");
    private static final UUID REPORT_ID_ANSWER = UUID.fromString("ee000000-0000-0000-0000-000000000002");
    private static final UUID REPORT_ID_CONTENT = UUID.fromString("ee000000-0000-0000-0000-000000000003");
    private static final UUID QUESTION_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID ANSWER_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID CONTENT_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = MODERATOR_ID::toString;
        org.mockito.Mockito.lenient().when(contentReportRepository.save(any(ContentReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static ContentReport makeReport(UUID id, ReportTargetType targetType, UUID targetId,
            ReportStatus status, Consumer<ContentReport> overrides) {
        ContentReport r = ContentReport.builder()
                .id(id)
                .targetId(targetId)
                .targetType(targetType)
                .status(status)
                .category("INAPPROPRIATE_CONTENT")
                .description("Reported by community member")
                .reporterUserId(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        overrides.accept(r);
        return r;
    }

    private static CommunityQuestion makeQuestion(QuestionStatus status) {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Test question")
                .body("Test body")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.LOW)
                .status(status)
                .build();
    }

    private static CommunityAnswer makeAnswer(AnswerStatus status) {
        return CommunityAnswer.builder()
                .id(ANSWER_ID)
                .questionId(QUESTION_ID)
                .authorId(UUID.randomUUID())
                .body("Test answer")
                .personalExperience(false)
                .status(status)
                .build();
    }

    // RES-TC-101: DISMISS a PENDING report -> status DISMISSED, no ModerationAction
    @Test
    void resolveReport_dismiss_setsDismissedAndNoAction() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, "Không vi phạm chính sách");
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(response.actionId()).isNull();
        assertThat(response.actionType()).isNull();
        assertThat(response.resultingStatus()).isNull();
        assertThat(response.resolvedByModeratorId()).isEqualTo(MODERATOR_ID);
        assertThat(response.resolvedAt()).isNotNull();

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        assertThat(captor.getValue().getAssignedModeratorId()).isEqualTo(MODERATOR_ID);

        verifyNoInteractions(moderationActionRepository);
        verify(auditService, times(1)).log(
                eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), eq("QUESTION"), eq(QUESTION_ID.toString()), any());
    }

    // RES-TC-102: HIDE a report (targetType=QUESTION) -> RESOLVED + action reportId set
    @Test
    void resolveReport_hideQuestion_setsResolvedAndActionWithReportId() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.PENDING)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> {
            ModerationAction a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.HIDE, "Nội dung chứa tư vấn y tế sai lệch");
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.actionId()).isNotNull();
        assertThat(response.resultingStatus()).isEqualTo("HIDDEN");

        ArgumentCaptor<CommunityQuestion> questionCaptor = ArgumentCaptor.forClass(CommunityQuestion.class);
        verify(communityQuestionRepository).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo(QuestionStatus.HIDDEN);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isEqualTo(REPORT_ID_QUESTION);

        ArgumentCaptor<ContentReport> reportCaptor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(reportCaptor.getValue().getResolvedAt()).isNotNull();
        assertThat(reportCaptor.getValue().getAssignedModeratorId()).isEqualTo(MODERATOR_ID);
    }

    // RES-TC-103: APPROVE a report (targetType=ANSWER) -> RESOLVED
    @Test
    void resolveReport_approveAnswer_setsResolved() {
        ContentReport report = makeReport(REPORT_ID_ANSWER, ReportTargetType.ANSWER, ANSWER_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_ANSWER)).thenReturn(Optional.of(report));
        when(communityAnswerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(makeAnswer(AnswerStatus.PENDING)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.APPROVE, null);
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_ANSWER, request, principal);

        assertThat(response.resultingStatus()).isEqualTo("APPROVED");
        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isEqualTo(REPORT_ID_ANSWER);
        assertThat(actionCaptor.getValue().getReason()).isNull();
    }

    // RES-TC-104: LOCK a report (targetType=QUESTION) -> LOCKED
    @Test
    void resolveReport_lockQuestion_setsLocked() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.APPROVED)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.LOCK, "Tranh cãi kéo dài");
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        assertThat(response.resultingStatus()).isEqualTo("LOCKED");
        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isEqualTo(REPORT_ID_QUESTION);
    }

    // RES-TC-105: LOCK on report targetType=ANSWER -> MOD-008 (reused UC-100 matrix)
    @Test
    void resolveReport_lockAnswer_throwsMod008() {
        ContentReport report = makeReport(REPORT_ID_ANSWER, ReportTargetType.ANSWER, ANSWER_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_ANSWER)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.LOCK, "test");

        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_ANSWER, request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-008");

        verify(contentReportRepository, never()).save(any());
    }

    // RES-TC-106: targetType=CONTENT + outcome in {APPROVE,HIDE,LOCK} -> MOD-012
    @Test
    void resolveReport_contentActionOnContentReport_throwsMod012() {
        ContentReport report = makeReport(REPORT_ID_CONTENT, ReportTargetType.CONTENT, CONTENT_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_CONTENT)).thenReturn(Optional.of(report));

        for (ResolutionOutcome outcome : new ResolutionOutcome[] {
                ResolutionOutcome.APPROVE, ResolutionOutcome.HIDE, ResolutionOutcome.LOCK}) {
            ResolveReportRequest request = new ResolveReportRequest(outcome, null);
            assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_CONTENT, request, principal))
                    .isInstanceOf(ModerationException.class)
                    .extracting(ex -> ((ModerationException) ex).getCode())
                    .isEqualTo("MOD-012");
        }

        verify(contentReportRepository, never()).save(any());
        verifyNoInteractions(communityQuestionRepository, communityAnswerRepository, moderationActionRepository);
    }

    // RES-TC-107: targetType=CONTENT + outcome=DISMISS -> still succeeds
    @Test
    void resolveReport_dismissContentReport_succeeds() {
        ContentReport report = makeReport(REPORT_ID_CONTENT, ReportTargetType.CONTENT, CONTENT_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_CONTENT)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, "Not a violation");
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_CONTENT, request, principal);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.DISMISSED);
        verifyNoInteractions(communityQuestionRepository, communityAnswerRepository);
    }

    // RES-TC-108: outcome in {WARN, SUSPEND} (any targetType) -> MOD-013
    @Test
    void resolveReport_warnOrSuspend_throwsMod013() {
        ContentReport questionReport = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(questionReport));

        for (ResolutionOutcome outcome : new ResolutionOutcome[] {ResolutionOutcome.WARN, ResolutionOutcome.SUSPEND}) {
            ResolveReportRequest request = new ResolveReportRequest(outcome, "First offense");
            assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, request, principal))
                    .isInstanceOf(ModerationException.class)
                    .extracting(ex -> ((ModerationException) ex).getCode())
                    .isEqualTo("MOD-013");
        }

        verify(contentReportRepository, never()).save(any());
        verifyNoInteractions(moderationActionRepository);
    }

    // RES-TC-109: reportId does not exist -> MOD-003
    @Test
    void resolveReport_reportNotFound_throwsMod003() {
        UUID unknownId = UUID.randomUUID();
        when(contentReportRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, null);

        assertThatThrownBy(() -> moderationService.resolveReport(unknownId, request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-003");
    }

    // RES-TC-110: report already RESOLVED/DISMISSED -> MOD-011 (re-resolution guard)
    @Test
    void resolveReport_alreadyResolvedOrDismissed_throwsMod011() {
        Instant originalResolvedAt = Instant.now().minusSeconds(600);
        UUID originalModerator = UUID.randomUUID();

        ContentReport resolvedReport = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.RESOLVED, r -> {
                    r.setResolvedAt(originalResolvedAt);
                    r.setAssignedModeratorId(originalModerator);
                });
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(resolvedReport));

        ResolveReportRequest dismissRequest = new ResolveReportRequest(ResolutionOutcome.DISMISS, null);
        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, dismissRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-011");

        ContentReport dismissedReport = makeReport(REPORT_ID_ANSWER, ReportTargetType.ANSWER, ANSWER_ID,
                ReportStatus.DISMISSED, r -> {});
        when(contentReportRepository.findById(REPORT_ID_ANSWER)).thenReturn(Optional.of(dismissedReport));

        ResolveReportRequest hideRequest = new ResolveReportRequest(ResolutionOutcome.HIDE, "test");
        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_ANSWER, hideRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-011");

        assertThat(resolvedReport.getResolvedAt()).isEqualTo(originalResolvedAt);
        assertThat(resolvedReport.getAssignedModeratorId()).isEqualTo(originalModerator);
        verify(contentReportRepository, never()).save(any());
    }

    // RES-TC-111: HIDE/LOCK missing reason -> MOD-010 (reused)
    @Test
    void resolveReport_hideOrLockWithoutReason_throwsMod010() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));

        ResolveReportRequest hideNull = new ResolveReportRequest(ResolutionOutcome.HIDE, null);
        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, hideNull, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");

        ResolveReportRequest hideBlank = new ResolveReportRequest(ResolutionOutcome.HIDE, "   ");
        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, hideBlank, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");

        ResolveReportRequest lockNull = new ResolveReportRequest(ResolutionOutcome.LOCK, null);
        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, lockNull, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");
    }

    // RES-TC-112: DISMISS does not require reason -> still succeeds
    @Test
    void resolveReport_dismissWithoutReason_succeeds() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, null);
        ResolveReportResponse response = moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    // RES-TC-113: ModerationAction.reportId always == report.id for action outcomes
    @Test
    void resolveReport_actionOutcome_alwaysSetsReportIdOnModerationAction() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.APPROVED)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.HIDE, "Vi phạm");
        moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isEqualTo(REPORT_ID_QUESTION);
    }

    // RES-TC-114: resolvedAt/assignedModeratorId set for DISMISS branch
    @Test
    void resolveReport_dismiss_setsResolvedAtAndAssignedModerator() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, null);
        moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(captor.capture());
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        assertThat(captor.getValue().getAssignedModeratorId()).isEqualTo(MODERATOR_ID);
    }

    // RES-TC-115: resolvedAt/assignedModeratorId set for RESOLVED branch
    @Test
    void resolveReport_actionOutcome_setsResolvedAtAndAssignedModerator() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.PENDING)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.APPROVE, null);
        moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(captor.capture());
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        assertThat(captor.getValue().getAssignedModeratorId()).isEqualTo(MODERATOR_ID);
    }

    // RES-TC-116: AuditService.log() called exactly once for DISMISS
    @Test
    void resolveReport_dismiss_auditLoggedExactlyOnce() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.DISMISS, null);
        moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), any(), any(), any());
    }

    // RES-TC-117: AuditService.log() called exactly once for action outcome (not double-logged by
    // the shared applyContentAction() primitive)
    @Test
    void resolveReport_actionOutcome_auditLoggedExactlyOnce() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.PENDING)));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.HIDE, "Policy violation");
        moderationService.resolveReport(REPORT_ID_QUESTION, request, principal);

        verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), any(), any(), any());
    }

    // RES-TC-118: report.targetId stale (deleted) when outcome=action -> MOD-007 (reused)
    @Test
    void resolveReport_staleTargetId_throwsMod007() {
        UUID unknownQuestionId = UUID.randomUUID();
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, unknownQuestionId,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(unknownQuestionId)).thenReturn(Optional.empty());

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.APPROVE, null);

        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-007");

        verify(contentReportRepository, never()).save(any());
    }

    // RES-TC-INT-003 (adapted — no Testcontainers/real-DB harness exists in this codebase, same
    // adaptation as UC-100's MOD-TC-INT-002). Verifies the necessary precondition for rollback: an
    // exception during the ModerationAction save propagates out of resolveReport() BEFORE the
    // ContentReport is marked RESOLVED, which is what allows @Transactional to roll back both writes.
    @Test
    void resolveReport_failureDuringActionSave_propagatesExceptionBeforeReportSave() {
        ContentReport report = makeReport(REPORT_ID_QUESTION, ReportTargetType.QUESTION, QUESTION_ID,
                ReportStatus.PENDING, r -> {});
        when(contentReportRepository.findById(REPORT_ID_QUESTION)).thenReturn(Optional.of(report));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeQuestion(QuestionStatus.PENDING)));
        when(moderationActionRepository.save(any(ModerationAction.class)))
                .thenThrow(new RuntimeException("simulated DB failure"));

        ResolveReportRequest request = new ResolveReportRequest(ResolutionOutcome.APPROVE, null);

        assertThatThrownBy(() -> moderationService.resolveReport(REPORT_ID_QUESTION, request, principal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated DB failure");

        verify(contentReportRepository, never()).save(any());
    }
}
