package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionLockedException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.file.service.IFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// UC-170: Delete Community Post
@ExtendWith(MockitoExtension.class)
class CommunityQuestionDeleteServiceImplTest {

    @Mock CommunityQuestionRepository questionRepository;
    @Mock CommunityTopicRepository topicRepository;
    @Mock CommunityAnswerRepository answerRepository;
    @Mock CommunityQuestionMapper questionMapper;
    @Mock AuditService auditService;
    @Mock IFileService fileService;
    @InjectMocks CommunityQuestionServiceImpl questionService;

    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private CommunityQuestion makeQuestion(QuestionStatus status) {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .authorId(AUTHOR_ID)
                .topicId(UUID.randomUUID())
                .title("Title")
                .body("Body content here")
                .imageUrls(List.of("https://res.cloudinary.com/demo/image/upload/question.jpg"))
                .status(status)
                .build();
    }

    // TC-170-1 / TC-170-2: author deletes own APPROVED/PENDING question -> status=DELETED
    @Test
    void deleteQuestion_authorOwnApprovedQuestion_setsDeletedStatus() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

        verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
    }

    @Test
    void deleteQuestion_authorOwnPendingQuestion_setsDeletedStatus() {
        CommunityQuestion question = makeQuestion(QuestionStatus.PENDING);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

        verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
    }

    // TC-170-3: author can delete own HIDDEN question (BR-COM-170-1 only blocks LOCKED, per Logic Issue L2)
    @Test
    void deleteQuestion_authorOwnHiddenQuestion_setsDeletedStatus() {
        CommunityQuestion question = makeQuestion(QuestionStatus.HIDDEN);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

        verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
    }

    // TC-170-3 sibling: LOCKED question cannot be deleted
    @Test
    void deleteQuestion_lockedQuestion_throwsQuestionLockedException() {
        CommunityQuestion question = makeQuestion(QuestionStatus.LOCKED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false))
                .isInstanceOf(QuestionLockedException.class)
                .hasMessageContaining("COM-012");

        verify(questionRepository, never()).save(any());
    }

    // TC-170-4: non-owner, non-moderator -> 403
    @Test
    void deleteQuestion_nonOwnerNonModerator_throwsAccessDenied() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.deleteQuestion(QUESTION_ID, OTHER_USER_ID, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(questionRepository, never()).save(any());
    }

    // TC-170-5: MODERATOR bypasses ownership check
    @Test
    void deleteQuestion_moderatorDeletesOthersQuestion_succeeds() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, OTHER_USER_ID, true);

        verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
        verify(fileService).purgeCommunityImages(question.getImageUrls(), AUTHOR_ID);
    }

    @Test
    void deleteQuestion_purgesQuestionAndAnswerImagesUsingEachContentAuthor() {
        UUID answerAuthorId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        var answer = com.carebridge.backend.community.entity.CommunityAnswer.builder()
                .questionId(QUESTION_ID)
                .authorId(answerAuthorId)
                .imageUrls(List.of("https://res.cloudinary.com/demo/image/upload/answer.jpg"))
                .build();
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(answerRepository.findAllByQuestionId(QUESTION_ID)).thenReturn(List.of(answer));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, OTHER_USER_ID, true);

        verify(fileService).purgeCommunityImages(question.getImageUrls(), AUTHOR_ID);
        verify(fileService).purgeCommunityImages(answer.getImageUrls(), answerAuthorId);
    }

    // TC-170-6: question not found -> 404
    @Test
    void deleteQuestion_questionNotFound_throwsQuestionNotFoundException() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    // TC-170-8: deleting an already-DELETED question is idempotent (no exception, still saves DELETED)
    @Test
    void deleteQuestion_alreadyDeleted_idempotent() {
        CommunityQuestion question = makeQuestion(QuestionStatus.DELETED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

        verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
        verifyNoInteractions(fileService);
    }

    // Audit log emitted with COMMUNITY_QUESTION_DELETED
    @Test
    void deleteQuestion_success_auditLogEmitted() {
        CommunityQuestion question = makeQuestion(QuestionStatus.APPROVED);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenReturn(question);

        questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

        verify(auditService).log(
                eq(AuditAction.COMMUNITY_QUESTION_DELETED),
                eq(AUTHOR_ID),
                eq("CommunityQuestion"),
                any(),
                eq("deleted")
        );
    }
}
