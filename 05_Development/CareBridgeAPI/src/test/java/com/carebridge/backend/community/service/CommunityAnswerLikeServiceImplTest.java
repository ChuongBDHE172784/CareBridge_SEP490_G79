package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.LikeToggleResponse;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityAnswerLike;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityAnswerLikeServiceImplTest {

    @Mock CommunityAnswerLikeRepository likeRepository;
    @Mock CommunityAnswerRepository answerRepository;
    @Mock AuditService auditService;
    @Mock CommunitySafetyPolicy communitySafetyPolicy;
    @InjectMocks CommunityAnswerLikeServiceImpl likeService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANSWER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CommunityAnswer makeAnswer(int likeCount) {
        return CommunityAnswer.builder()
                .id(ANSWER_ID)
                .questionId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .body("Answer body content")
                .status(AnswerStatus.APPROVED)
                .likeCount(likeCount)
                .build();
    }

    @Test
    void toggleLike_notYetLiked_addsLike() {
        CommunityAnswer answer = makeAnswer(5);
        when(communitySafetyPolicy.requireVisibleAnswer(USER_ID, ANSWER_ID)).thenReturn(answer);
        when(likeRepository.existsByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(false);
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LikeToggleResponse result = likeService.toggleLike(USER_ID, ANSWER_ID);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(6);
        assertThat(result.getAnswerId()).isEqualTo(ANSWER_ID);
        verify(likeRepository).save(any(CommunityAnswerLike.class));
        verify(answerRepository).save(any(CommunityAnswer.class));
    }

    @Test
    void toggleLike_alreadyLiked_removesLike() {
        CommunityAnswer answer = makeAnswer(3);
        CommunityAnswerLike existingLike = CommunityAnswerLike.builder()
                .id(UUID.randomUUID()).userId(USER_ID).answerId(ANSWER_ID).build();
        when(communitySafetyPolicy.requireVisibleAnswer(USER_ID, ANSWER_ID)).thenReturn(answer);
        when(likeRepository.existsByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(true);
        when(likeRepository.findByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(Optional.of(existingLike));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LikeToggleResponse result = likeService.toggleLike(USER_ID, ANSWER_ID);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.getLikeCount()).isEqualTo(2);
        verify(likeRepository).delete(existingLike);
    }

    @Test
    void toggleLike_answerNotFound_throws() {
        when(communitySafetyPolicy.requireVisibleAnswer(USER_ID, ANSWER_ID))
                .thenThrow(new AnswerNotFoundException(ANSWER_ID.toString()));

        assertThatThrownBy(() -> likeService.toggleLike(USER_ID, ANSWER_ID))
                .isInstanceOf(AnswerNotFoundException.class);
    }

    @Test
    void toggleLike_likeCountAtZero_doesNotGoNegative() {
        CommunityAnswer answer = makeAnswer(0);
        CommunityAnswerLike existingLike = CommunityAnswerLike.builder()
                .id(UUID.randomUUID()).userId(USER_ID).answerId(ANSWER_ID).build();
        when(communitySafetyPolicy.requireVisibleAnswer(USER_ID, ANSWER_ID)).thenReturn(answer);
        when(likeRepository.existsByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(true);
        when(likeRepository.findByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(Optional.of(existingLike));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LikeToggleResponse result = likeService.toggleLike(USER_ID, ANSWER_ID);

        assertThat(result.getLikeCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void toggleLike_returnsCorrectAnswerId() {
        CommunityAnswer answer = makeAnswer(1);
        when(communitySafetyPolicy.requireVisibleAnswer(USER_ID, ANSWER_ID)).thenReturn(answer);
        when(likeRepository.existsByUserIdAndAnswerId(USER_ID, ANSWER_ID)).thenReturn(false);
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LikeToggleResponse result = likeService.toggleLike(USER_ID, ANSWER_ID);

        assertThat(result.getAnswerId()).isEqualTo(ANSWER_ID);
    }
}
