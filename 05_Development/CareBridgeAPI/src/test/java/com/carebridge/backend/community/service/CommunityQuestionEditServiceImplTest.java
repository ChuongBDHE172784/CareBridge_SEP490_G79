package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotEditableException;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityQuestionEditServiceImplTest {

    @Mock CommunityQuestionRepository questionRepository;
    @Mock CommunityTopicRepository topicRepository;
    @Mock CommunityQuestionMapper questionMapper;
    @Mock AuditService auditService;
    @Mock CommunitySafetyPolicy communitySafetyPolicy;
    @Mock
    private com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;

    @InjectMocks CommunityQuestionServiceImpl questionService;

    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private CommunityQuestion makeQuestion(QuestionStatus status) {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .authorId(AUTHOR_ID)
                .topicId(UUID.randomUUID())
                .title("Original title")
                .body("Original body content here")
                .status(status)
                .anonymous(false)
                .build();
    }

    private UpdateCommunityQuestionRequest makeRequest() {
        UpdateCommunityQuestionRequest req = new UpdateCommunityQuestionRequest();
        req.setTitle("Updated title");
        req.setBody("Updated body content here minimum 10 chars");
        return req;
    }

    @Test
    void editQuestion_happyPath_approvedQuestion_returnsUpdated() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        CommunityQuestionResponse expectedResponse = CommunityQuestionResponse.builder()
                .id(QUESTION_ID).title("Updated title").build();
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);
        when(questionMapper.toResponse(any())).thenReturn(expectedResponse);

        CommunityQuestionResponse result = questionService.editQuestion(AUTHOR_ID, QUESTION_ID, makeRequest());

        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void editQuestion_pendingQuestion_success() {
        CommunityQuestion question = makeQuestion(QuestionStatus.PENDING);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);
        when(questionMapper.toResponse(any())).thenReturn(CommunityQuestionResponse.builder().id(QUESTION_ID).build());

        CommunityQuestionResponse result = questionService.editQuestion(AUTHOR_ID, QUESTION_ID, makeRequest());

        assertThat(result).isNotNull();
    }

    @Test
    void editQuestion_nonAuthor_throwsAccessDenied() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.editQuestion(OTHER_USER_ID, QUESTION_ID, makeRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editQuestion_lockedQuestion_throwsNotEditable() {
        CommunityQuestion question = makeQuestion(QuestionStatus.LOCKED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.editQuestion(AUTHOR_ID, QUESTION_ID, makeRequest()))
                .isInstanceOf(QuestionNotEditableException.class);
    }

    @Test
    void editQuestion_hiddenQuestion_throwsNotEditable() {
        CommunityQuestion question = makeQuestion(QuestionStatus.HIDDEN);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.editQuestion(AUTHOR_ID, QUESTION_ID, makeRequest()))
                .isInstanceOf(QuestionNotEditableException.class);
    }

    @Test
    void editQuestion_questionNotFound_throwsNotFound() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.editQuestion(AUTHOR_ID, QUESTION_ID, makeRequest()))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void editQuestion_toggleAnonymousToTrue_authorIdNullInResponse() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        UpdateCommunityQuestionRequest req = makeRequest();
        req.setIsAnonymous(true);
        CommunityQuestionResponse expectedResponse = CommunityQuestionResponse.builder()
                .id(QUESTION_ID).anonymous(true).authorId(null).build();
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);
        when(questionMapper.toResponse(any())).thenReturn(expectedResponse);

        CommunityQuestionResponse result = questionService.editQuestion(AUTHOR_ID, QUESTION_ID, req);

        assertThat(result.isAnonymous()).isTrue();
        assertThat(result.getAuthorId()).isNull();
    }

    @Test
    void editQuestion_partialUpdate_onlyTitleChanged() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        UpdateCommunityQuestionRequest req = new UpdateCommunityQuestionRequest();
        req.setTitle("New title only");
        // body and urgency are null — should not be overwritten
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(CommunityQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionMapper.toResponse(any())).thenAnswer(inv -> {
            CommunityQuestion q = inv.getArgument(0);
            return CommunityQuestionResponse.builder().id(q.getId()).title(q.getTitle()).build();
        });

        CommunityQuestionResponse result = questionService.editQuestion(AUTHOR_ID, QUESTION_ID, req);

        assertThat(result).isNotNull();
        // body should still be original since req.body is null
        verify(questionRepository).save(argThat(q ->
            "New title only".equals(q.getTitle()) && "Original body content here".equals(q.getBody())
        ));
    }
}
