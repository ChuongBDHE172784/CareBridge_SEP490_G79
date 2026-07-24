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
import com.carebridge.backend.content.dto.request.ModerateContentRequest;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import java.security.Principal;
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

// MOD-TC-101..112, MOD-TC-INT-002 (adapted — see class-level note)
@ExtendWith(MockitoExtension.class)
class ModerateContentServiceImplTest {

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

    // === MOD-TC Props Isolation Pattern (Test-Spec §4) ===
    private static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID ANSWER_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID CONTENT_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = MODERATOR_ID::toString;
    }

    private static CommunityQuestion makeQuestion(Consumer<CommunityQuestion> overrides) {
        CommunityQuestion q = CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Test question")
                .body("Test body")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.LOW)
                .status(QuestionStatus.PENDING)
                .build();
        overrides.accept(q);
        return q;
    }

    private static CommunityAnswer makeAnswer(Consumer<CommunityAnswer> overrides) {
        CommunityAnswer a = CommunityAnswer.builder()
                .id(ANSWER_ID)
                .questionId(QUESTION_ID)
                .authorId(UUID.randomUUID())
                .body("Test answer")
                .personalExperience(false)
                .status(AnswerStatus.PENDING)
                .build();
        overrides.accept(a);
        return a;
    }

    private static ModerateContentRequest makeRequest(
            UUID targetId, ReportTargetType targetType, ModerationActionType actionType, String reason) {
        return new ModerateContentRequest(targetId, targetType, actionType, reason);
    }

    // MOD-TC-101: APPROVE a PENDING QUESTION → status APPROVED, action recorded
    @Test
    void moderateContent_approveQuestion_updatesStatusAndRecordsAction() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.PENDING));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class)))
                .thenAnswer(inv -> {
                    ModerationAction a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);

        ModerateContentResponse response = moderationService.moderateContent(request, principal);

        assertThat(response.actionId()).isNotNull();
        assertThat(response.resultingStatus()).isEqualTo("APPROVED");
        assertThat(response.actionType()).isEqualTo(ModerationActionType.APPROVE);
        assertThat(response.targetType()).isEqualTo(ReportTargetType.QUESTION);
        assertThat(response.targetId()).isEqualTo(QUESTION_ID);
        assertThat(response.moderatorUserId()).isEqualTo(MODERATOR_ID);
        assertThat(response.actionAt()).isNotNull();

        ArgumentCaptor<CommunityQuestion> questionCaptor = ArgumentCaptor.forClass(CommunityQuestion.class);
        verify(communityQuestionRepository, times(1)).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo(QuestionStatus.APPROVED);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository, times(1)).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isNull();
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ModerationActionType.APPROVE);

        verify(auditService, times(1)).log(
                eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), eq("QUESTION"), eq(QUESTION_ID.toString()), any());
    }

    // MOD-TC-102: HIDE a QUESTION (with reason) → status HIDDEN
    @Test
    void moderateContent_hideQuestion_updatesStatusToHidden() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.APPROVED));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, "Nội dung không phù hợp");

        ModerateContentResponse response = moderationService.moderateContent(request, principal);

        assertThat(response.resultingStatus()).isEqualTo("HIDDEN");

        ArgumentCaptor<CommunityQuestion> questionCaptor = ArgumentCaptor.forClass(CommunityQuestion.class);
        verify(communityQuestionRepository).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo(QuestionStatus.HIDDEN);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReason()).isEqualTo("Nội dung không phù hợp");
    }

    // MOD-TC-103: LOCK a QUESTION (with reason) → status LOCKED
    @Test
    void moderateContent_lockQuestion_updatesStatusToLocked() {
        when(communityQuestionRepository.lockIfApproved(QUESTION_ID)).thenReturn(1);
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, "Tranh cãi kéo dài, cần dừng trả lời mới");

        ModerateContentResponse response = moderationService.moderateContent(request, principal);

        assertThat(response.resultingStatus()).isEqualTo("LOCKED");
        verify(communityQuestionRepository).lockIfApproved(QUESTION_ID);
    }

    // MOD-TC-104: APPROVE/HIDE an ANSWER → status tương ứng
    @Test
    void moderateContent_approveAndHideAnswer_updatesStatusRespectively() {
        CommunityAnswer answer = makeAnswer(a -> a.setStatus(AnswerStatus.PENDING));
        when(communityAnswerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest approveRequest = makeRequest(ANSWER_ID, ReportTargetType.ANSWER,
                ModerationActionType.APPROVE, null);
        ModerateContentResponse approveResponse = moderationService.moderateContent(approveRequest, principal);
        assertThat(approveResponse.resultingStatus()).isEqualTo("APPROVED");

        CommunityAnswer approvedAnswer = makeAnswer(a -> a.setStatus(AnswerStatus.APPROVED));
        when(communityAnswerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(approvedAnswer));

        ModerateContentRequest hideRequest = makeRequest(ANSWER_ID, ReportTargetType.ANSWER,
                ModerationActionType.HIDE, "Spam");
        ModerateContentResponse hideResponse = moderationService.moderateContent(hideRequest, principal);
        assertThat(hideResponse.resultingStatus()).isEqualTo("HIDDEN");

        verify(communityAnswerRepository, times(2)).save(any(CommunityAnswer.class));
        verify(moderationActionRepository, times(2)).save(any(ModerationAction.class));
    }

    @Test
    void moderateContent_requestRevisionAnswer_updatesStatusToPendingAndRecordsReason() {
        CommunityAnswer answer = makeAnswer(a -> a.setStatus(AnswerStatus.APPROVED));
        when(communityAnswerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(ANSWER_ID, ReportTargetType.ANSWER,
                ModerationActionType.REQUEST_REVISION, "Cần bổ sung nguồn và chỉnh lời khuyên an toàn hơn");

        ModerateContentResponse response = moderationService.moderateContent(request, principal);

        assertThat(response.resultingStatus()).isEqualTo("PENDING");
        ArgumentCaptor<CommunityAnswer> answerCaptor = ArgumentCaptor.forClass(CommunityAnswer.class);
        verify(communityAnswerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getStatus()).isEqualTo(AnswerStatus.PENDING);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ModerationActionType.REQUEST_REVISION);
        assertThat(actionCaptor.getValue().getReason()).contains("bổ sung nguồn");
    }

    // MOD-TC-105: LOCK on ANSWER → 400 MOD-008 (AnswerStatus has no LOCKED value)
    @Test
    void moderateContent_lockAnswer_throwsMod008AndNeverLooksUp() {
        ModerateContentRequest request = makeRequest(ANSWER_ID, ReportTargetType.ANSWER,
                ModerationActionType.LOCK, "test");

        assertThatThrownBy(() -> moderationService.moderateContent(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-008");

        verifyNoInteractions(communityAnswerRepository);
        verify(moderationActionRepository, never()).save(any());
    }

    // MOD-TC-106: targetType=CONTENT (any actionType) → rejected with MOD-009
    @Test
    void moderateContent_targetTypeContent_alwaysRejectedWithMod009() {
        for (ModerationActionType actionType : new ModerationActionType[] {
                ModerationActionType.APPROVE, ModerationActionType.HIDE, ModerationActionType.LOCK}) {
            ModerateContentRequest request = makeRequest(CONTENT_ID, ReportTargetType.CONTENT, actionType, null);

            assertThatThrownBy(() -> moderationService.moderateContent(request, principal))
                    .isInstanceOf(ModerationException.class)
                    .extracting(ex -> ((ModerationException) ex).getCode())
                    .isEqualTo("MOD-009");
        }

        verifyNoInteractions(communityQuestionRepository, communityAnswerRepository, contentReportRepository);
    }

    // MOD-TC-107: actionType WARN/SUSPEND at this endpoint → 400 MOD-009
    @Test
    void moderateContent_warnOrSuspendActionType_throwsMod009() {
        ModerateContentRequest warnRequest = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.WARN, "test");
        assertThatThrownBy(() -> moderationService.moderateContent(warnRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-009");

        ModerateContentRequest suspendRequest = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.SUSPEND, "test");
        assertThatThrownBy(() -> moderationService.moderateContent(suspendRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-009");

        verifyNoInteractions(communityQuestionRepository);
    }

    // UNDO-TC-017 (CB-MOD-IMP-009): actionType=UNDO at this endpoint → 400 MOD-009 — UNDO may only
    // be created via ModerationServiceImpl.undoModerationAction()'s dedicated guarded path (ADR-005 §C7)
    @Test
    void moderateContent_undoActionType_throwsMod009() {
        ModerateContentRequest undoRequest = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.UNDO, null);

        assertThatThrownBy(() -> moderationService.moderateContent(undoRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-009");

        verifyNoInteractions(communityQuestionRepository);
    }

    // MOD-TC-108: HIDE/LOCK missing reason → 400 MOD-010
    @Test
    void moderateContent_hideOrLockWithoutReason_throwsMod010() {
        ModerateContentRequest hideNull = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, null);
        assertThatThrownBy(() -> moderationService.moderateContent(hideNull, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");

        ModerateContentRequest hideBlank = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, "   ");
        assertThatThrownBy(() -> moderationService.moderateContent(hideBlank, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");

        ModerateContentRequest lockNull = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, null);
        assertThatThrownBy(() -> moderationService.moderateContent(lockNull, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-010");
    }

    // MOD-TC-109: APPROVE does not require reason → still succeeds
    @Test
    void moderateContent_approveWithoutReason_succeeds() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.PENDING));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);

        ModerateContentResponse response = moderationService.moderateContent(request, principal);

        assertThat(response.resultingStatus()).isEqualTo("APPROVED");
        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReason()).isNull();
    }

    // MOD-TC-110: targetId not found → 404 MOD-007
    @Test
    void moderateContent_targetNotFound_throwsMod007() {
        UUID unknownId = UUID.randomUUID();
        when(communityQuestionRepository.findById(unknownId)).thenReturn(Optional.empty());

        ModerateContentRequest request = makeRequest(unknownId, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);

        assertThatThrownBy(() -> moderationService.moderateContent(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-007");
    }

    // MOD-TC-111: ModerationAction.reportId is always null for actions created via UC-100
    @Test
    void moderateContent_createdAction_alwaysHasNullReportId() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.PENDING));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);
        moderationService.moderateContent(request, principal);

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getReportId()).isNull();
        verifyNoInteractions(contentReportRepository);
    }

    // MOD-TC-112: AuditService.log(MODERATION_ACTION, ...) called exactly once on success
    @Test
    void moderateContent_onSuccess_auditLoggedExactlyOnce() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.PENDING));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);
        moderationService.moderateContent(request, principal);

        verify(auditService, times(1)).log(
                eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), eq("QUESTION"), eq(QUESTION_ID.toString()), any());
    }

    // MOD-TC-INT-002 (adapted — no Testcontainers/real DB harness exists in this codebase; the
    // real rollback guarantee comes from @Transactional on moderateContent(), which framework
    // integration tests cannot exercise with mocked repositories. This unit test instead verifies
    // the necessary precondition for rollback: an exception from moderationActionRepository.save()
    // (after the status mutation is queued) propagates out of moderateContent() rather than being
    // swallowed, which is what allows the Spring transaction proxy to roll back both writes.)
    @Test
    void moderateContent_failureAfterStatusMutation_propagatesException() {
        CommunityQuestion question = makeQuestion(q -> q.setStatus(QuestionStatus.PENDING));
        when(communityQuestionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class)))
                .thenThrow(new RuntimeException("simulated DB failure"));

        ModerateContentRequest request = makeRequest(QUESTION_ID, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, null);

        assertThatThrownBy(() -> moderationService.moderateContent(request, principal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated DB failure");
    }
}
