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
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;

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

@ExtendWith(MockitoExtension.class)
class CommunityAnswerServiceImplTest {

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Spy
    private CommunityAnswerMapper answerMapper = new CommunityAnswerMapper();

    @Mock
    private AuditService auditService;

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
                org.mockito.ArgumentMatchers.eq("posted")
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
}
