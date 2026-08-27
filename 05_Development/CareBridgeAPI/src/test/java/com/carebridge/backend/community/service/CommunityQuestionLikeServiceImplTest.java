package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.QuestionLikeToggleResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityQuestionLike;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityQuestionLikeServiceImplTest {

    @Mock CommunityQuestionLikeRepository likeRepository;
    @Mock CommunityQuestionRepository questionRepository;
    @Mock AuditService auditService;
    @Mock CommunitySafetyPolicy communitySafetyPolicy;
    @InjectMocks CommunityQuestionLikeServiceImpl likeService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CommunityQuestion makeQuestion(int likeCount) {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Test question title")
                .body("Test question body")
                .status(QuestionStatus.APPROVED)
                .likeCount(likeCount)
                .build();
    }

    // COMQL-TC-001
    @Test
    void toggleLike_notYetLiked_addsLike() {
        CommunityQuestion question = makeQuestion(2);
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(question);
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionLikeToggleResponse result = likeService.toggleLike(USER_ID, QUESTION_ID);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(3);
        assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
        verify(likeRepository).save(any(CommunityQuestionLike.class));
        verify(questionRepository).save(any(CommunityQuestion.class));
        verify(auditService).log(eq(com.carebridge.backend.audit.entity.AuditAction.COMMUNITY_QUESTION_LIKED),
                eq(USER_ID), anyString(), anyString(), anyString());
    }

    // COMQL-TC-002
    @Test
    void toggleLike_alreadyLiked_removesLike() {
        CommunityQuestion question = makeQuestion(3);
        CommunityQuestionLike existingLike = CommunityQuestionLike.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(question);
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(true);
        when(likeRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(Optional.of(existingLike));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionLikeToggleResponse result = likeService.toggleLike(USER_ID, QUESTION_ID);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.getLikeCount()).isEqualTo(2);
        verify(likeRepository).delete(existingLike);
        verify(likeRepository, never()).save(any());
        verify(auditService).log(eq(com.carebridge.backend.audit.entity.AuditAction.COMMUNITY_QUESTION_UNLIKED),
                eq(USER_ID), anyString(), anyString(), anyString());
    }

    // COMQL-TC-003
    @Test
    void toggleLike_questionNotFound_throws() {
        UUID unknownId = UUID.randomUUID();
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, unknownId))
                .thenThrow(new QuestionNotFoundException(unknownId.toString()));

        assertThatThrownBy(() -> likeService.toggleLike(USER_ID, unknownId))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining("COM-006");

        verify(likeRepository, never()).existsByUserIdAndQuestionId(any(), any());
        verify(likeRepository, never()).save(any());
        verify(questionRepository, never()).save(any());
    }

    // COMQL-TC-004
    @Test
    void toggleLike_calledTwice_secondCallUnlikes() {
        CommunityQuestion question = makeQuestion(2);
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(question);
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionLikeToggleResponse first = likeService.toggleLike(USER_ID, QUESTION_ID);
        assertThat(first.isLiked()).isTrue();
        assertThat(first.getLikeCount()).isEqualTo(3);

        CommunityQuestionLike existingLike = CommunityQuestionLike.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(true);
        when(likeRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(Optional.of(existingLike));

        QuestionLikeToggleResponse second = likeService.toggleLike(USER_ID, QUESTION_ID);
        assertThat(second.isLiked()).isFalse();
        assertThat(second.getLikeCount()).isEqualTo(2);
    }

    // COMQL-TC-006
    @Test
    void toggleLike_likeCountAtZero_doesNotGoNegative() {
        CommunityQuestion question = makeQuestion(0);
        CommunityQuestionLike existingLike = CommunityQuestionLike.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(question);
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(true);
        when(likeRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(Optional.of(existingLike));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionLikeToggleResponse result = likeService.toggleLike(USER_ID, QUESTION_ID);

        assertThat(result.getLikeCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.getLikeCount()).isZero();
    }

    // COMQL-TC-007
    @Test
    void toggleLike_responseLikeCountMatchesUpdatedValue() {
        CommunityQuestion question = makeQuestion(10);
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(question);
        when(likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionLikeToggleResponse result = likeService.toggleLike(USER_ID, QUESTION_ID);

        assertThat(result.getLikeCount()).isEqualTo(11);
    }
}
