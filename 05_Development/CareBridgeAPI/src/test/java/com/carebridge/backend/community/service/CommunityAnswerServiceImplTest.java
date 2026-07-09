package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.AnswerNotEditableException;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CommunityAnswerServiceImplTest {

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Mock
    private CommunityProfileRepository profileRepository;

    @Spy
    private CommunityAnswerMapper answerMapper = new CommunityAnswerMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private CommunitySafetyPolicy communitySafetyPolicy;

    @InjectMocks
    private CommunityAnswerServiceImpl service;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    private PostCommunityAnswerRequest makeRequest() {
        PostCommunityAnswerRequest req = new PostCommunityAnswerRequest();
        req.setBody("This is a valid personal experience answer with enough characters");
        req.setIsPersonalExperience(true);
        return req;
    }

    private PostCommunityAnswerRequest makeRequest(Consumer<PostCommunityAnswerRequest> overrides) {
        PostCommunityAnswerRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    private CommunityQuestion makeApprovedQuestion() {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .status(QuestionStatus.APPROVED)
                .build();
    }

    // COM56-TC-001: Happy path — status=PENDING, isExpertLabeled=false (ADR-COM-005, ADR-COM-006)
    @Test
    void postAnswer_validRequest_returnsCorrectDefaults() {
        when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
                .thenReturn(Optional.of(makeApprovedQuestion()));
        CommunityAnswer saved = CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(QUESTION_ID)
                .authorId(AUTHOR_ID)
                .body("This is a valid personal experience answer with enough characters")
                .status(AnswerStatus.PENDING)
                .expertLabeled(false)
                .personalExperience(true)
                .build();
        when(answerRepository.save(any())).thenReturn(saved);

        CommunityAnswerResponse response = service.postAnswer(AUTHOR_ID, QUESTION_ID, makeRequest());

        // ADR-COM-006: status must be PENDING, ADR-COM-005: isExpertLabeled must be false
        verify(answerRepository, times(1)).save(argThat(a ->
                a.getStatus() == AnswerStatus.PENDING
                && !a.isExpertLabeled()
                && a.getAuthorId().equals(AUTHOR_ID)
                && a.getQuestionId().equals(QUESTION_ID)
        ));
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.isExpertLabeled()).isFalse();
    }

    // COM56-TC-001 sub: audit event emitted after successful answer post
    @Test
    void postAnswer_validRequest_auditLogEmitted() {
        when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
                .thenReturn(Optional.of(makeApprovedQuestion()));
        CommunityAnswer saved = CommunityAnswer.builder()
                .id(UUID.randomUUID()).questionId(QUESTION_ID).authorId(AUTHOR_ID)
                .status(AnswerStatus.PENDING).expertLabeled(false).personalExperience(true)
                .build();
        when(answerRepository.save(any())).thenReturn(saved);

        service.postAnswer(AUTHOR_ID, QUESTION_ID, makeRequest());

        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.COMMUNITY_ANSWER_POSTED),
                org.mockito.ArgumentMatchers.eq(AUTHOR_ID),
                org.mockito.ArgumentMatchers.eq("CommunityAnswer"),
                any(),
                org.mockito.ArgumentMatchers.eq("posted expertLabeled=false")
        );
    }

    // COM56-TC-002: question not APPROVED → QuestionNotAnswerableException (COM-007) (ADR-COM-006)
    @ParameterizedTest
    @EnumSource(value = QuestionStatus.class, names = {"PENDING", "HIDDEN", "LOCKED"})
    void postAnswer_questionNotApproved_throwsQuestionNotAnswerableException(QuestionStatus status) {
        when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postAnswer(AUTHOR_ID, QUESTION_ID, makeRequest()))
                .isInstanceOf(QuestionNotAnswerableException.class)
                .hasMessageContaining("COM-007");

        verify(answerRepository, never()).save(any());
    }

    // COM56-TC: isPersonalExperience=false — still saves with expertLabeled=false (ADR-COM-005)
    @Test
    void postAnswer_nonPersonalExperience_expertLabeledStillFalse() {
        when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
                .thenReturn(Optional.of(makeApprovedQuestion()));
        CommunityAnswer saved = CommunityAnswer.builder()
                .id(UUID.randomUUID()).questionId(QUESTION_ID).authorId(AUTHOR_ID)
                .status(AnswerStatus.PENDING).expertLabeled(false).personalExperience(false)
                .build();
        when(answerRepository.save(any())).thenReturn(saved);

        CommunityAnswerResponse response = service.postAnswer(AUTHOR_ID, QUESTION_ID,
                makeRequest(req -> req.setIsPersonalExperience(false)));

        verify(answerRepository).save(argThat(a -> !a.isExpertLabeled()));
        assertThat(response.isExpertLabeled()).isFalse();
    }

    // ===================== UC-200: Edit Own Answer =====================

    private static final UUID ANSWER_ID = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private CommunityAnswer makeAnswer(AnswerStatus status) {
        return CommunityAnswer.builder()
                .id(ANSWER_ID)
                .questionId(QUESTION_ID)
                .authorId(AUTHOR_ID)
                .body("Original answer body")
                .personalExperience(false)
                .expertLabeled(false)
                .status(status)
                .build();
    }

    private EditAnswerRequest makeEditRequest() {
        EditAnswerRequest req = new EditAnswerRequest();
        req.setBody("Updated answer body");
        req.setIsPersonalExperience(true);
        return req;
    }

    // TC-200-1: author edits own APPROVED answer -> status resets to PENDING
    @Test
    void editAnswer_ownApprovedAnswer_resetsToPending() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommunityAnswerResponse response = service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getBody()).isEqualTo("Updated answer body");
        verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.PENDING));
    }

    // TC-200-2: author edits own PENDING answer -> stays PENDING
    @Test
    void editAnswer_ownPendingAnswer_staysPending() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.PENDING);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommunityAnswerResponse response = service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest());

        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    // TC-200-3: HIDDEN answer cannot be edited
    @Test
    void editAnswer_hiddenAnswer_throwsAnswerNotEditableException() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.HIDDEN);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest()))
                .isInstanceOf(AnswerNotEditableException.class)
                .hasMessageContaining("COM-013");

        verify(answerRepository, never()).save(any());
    }

    // DELETED answer cannot be edited either
    @Test
    void editAnswer_deletedAnswer_throwsAnswerNotEditableException() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.DELETED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest()))
                .isInstanceOf(AnswerNotEditableException.class);
    }

    // TC-200-4: non-owner attempts edit -> 403
    @Test
    void editAnswer_nonOwner_throwsAccessDenied() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.editAnswer(ANSWER_ID, OTHER_USER_ID, makeEditRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(answerRepository, never()).save(any());
    }

    // TC-200-7: edit non-existent answer -> 404
    @Test
    void editAnswer_answerNotFound_throwsAnswerNotFoundException() {
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest()))
                .isInstanceOf(AnswerNotFoundException.class);
    }

    // TC-200-9: expertLabeled must never change via edit
    @Test
    void editAnswer_expertLabeledNeverModified() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        answer.setExpertLabeled(true);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommunityAnswerResponse response = service.editAnswer(ANSWER_ID, AUTHOR_ID, makeEditRequest());

        assertThat(response.isExpertLabeled()).isTrue();
        verify(answerRepository).save(argThat(CommunityAnswer::isExpertLabeled));
    }

    // ===================== UC-201: Delete Own Answer =====================

    // TC-201-1: author deletes own APPROVED answer -> status=DELETED, answer_count decremented
    @Test
    void deleteAnswer_ownApprovedAnswer_decrementsAnswerCount() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

        verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
        verify(questionRepository).decrementAnswerCount(QUESTION_ID);
    }

    // TC-201-2 / TC-201-3: PENDING/HIDDEN answer deleted -> no decrement
    @Test
    void deleteAnswer_ownPendingAnswer_doesNotDecrementAnswerCount() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.PENDING);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

        verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
        verify(questionRepository, never()).decrementAnswerCount(any());
    }

    @Test
    void deleteAnswer_ownHiddenAnswer_doesNotDecrementAnswerCount() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.HIDDEN);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

        verify(questionRepository, never()).decrementAnswerCount(any());
    }

    // TC-201-4: non-owner, non-moderator -> 403
    @Test
    void deleteAnswer_nonOwnerNonModerator_throwsAccessDenied() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.deleteAnswer(ANSWER_ID, OTHER_USER_ID, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(answerRepository, never()).save(any());
    }

    // TC-201-5: MODERATOR bypasses ownership check
    @Test
    void deleteAnswer_moderatorDeletesOthersAnswer_succeeds() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, OTHER_USER_ID, true);

        verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
    }

    // TC-201-6: answer not found -> 404
    @Test
    void deleteAnswer_answerNotFound_throwsAnswerNotFoundException() {
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false))
                .isInstanceOf(AnswerNotFoundException.class);
    }

    // TC-201-8/9: already-DELETED answer is idempotent and never decrements again
    @Test
    void deleteAnswer_alreadyDeleted_idempotentNoDecrement() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.DELETED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

        verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
        verify(questionRepository, never()).decrementAnswerCount(any());
    }

    // Audit log emitted with COMMUNITY_ANSWER_DELETED
    @Test
    void deleteAnswer_success_auditLogEmitted() {
        CommunityAnswer answer = makeAnswer(AnswerStatus.APPROVED);
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.COMMUNITY_ANSWER_DELETED),
                org.mockito.ArgumentMatchers.eq(AUTHOR_ID),
                org.mockito.ArgumentMatchers.eq("CommunityAnswer"),
                any(),
                org.mockito.ArgumentMatchers.eq("deleted")
        );
    }
}
