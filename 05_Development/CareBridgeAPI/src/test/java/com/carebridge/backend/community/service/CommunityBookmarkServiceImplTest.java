package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.BookmarkToggleResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityBookmark;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityBookmarkServiceImplTest {

    @Mock CommunityBookmarkRepository bookmarkRepository;
    @Mock CommunityQuestionRepository questionRepository;
    @Mock CommunityTopicRepository topicRepository;
    @Mock CommunityAnswerRepository answerRepository;
    @Mock CommunityQuestionLikeRepository likeRepository;
    @Mock CommunityFeedMapper feedMapper;
    @Mock AuditService auditService;
    @Mock CommunitySafetyPolicy communitySafetyPolicy;
    @InjectMocks CommunityBookmarkServiceImpl bookmarkService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CommunityQuestion makeApprovedQuestion() {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Test Question")
                .body("Test question body")
                .status(QuestionStatus.APPROVED)
                .anonymous(false)
                .build();
    }

    @Test
    void toggleBookmark_notYetBookmarked_addsBookmark() {
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(makeApprovedQuestion());
        when(bookmarkRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(false);
        when(bookmarkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookmarkToggleResponse result = bookmarkService.toggleBookmark(USER_ID, QUESTION_ID);

        assertThat(result.isBookmarked()).isTrue();
        assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
        verify(bookmarkRepository).save(any(CommunityBookmark.class));
    }

    @Test
    void toggleBookmark_alreadyBookmarked_removesBookmark() {
        CommunityBookmark existing = CommunityBookmark.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID)).thenReturn(makeApprovedQuestion());
        when(bookmarkRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(true);
        when(bookmarkRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(Optional.of(existing));

        BookmarkToggleResponse result = bookmarkService.toggleBookmark(USER_ID, QUESTION_ID);

        assertThat(result.isBookmarked()).isFalse();
        verify(bookmarkRepository).delete(existing);
    }

    @Test
    void toggleBookmark_questionNotFound_throws() {
        when(communitySafetyPolicy.requireVisibleQuestion(USER_ID, QUESTION_ID))
                .thenThrow(new QuestionNotFoundException(QUESTION_ID.toString()));

        assertThatThrownBy(() -> bookmarkService.toggleBookmark(USER_ID, QUESTION_ID))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void getBookmarkedQuestions_returnsPagedFeedItems() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityBookmark bookmark = CommunityBookmark.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        Page<CommunityBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark));
        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(bookmarkPage);
        when(questionRepository.findAllById(anyCollection())).thenReturn(List.of(question));
        when(topicRepository.findAllById(any())).thenReturn(List.of());
        when(answerRepository.findQuestionIdsWithExpertAnswer(anyCollection())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(eq(USER_ID), anyCollection())).thenReturn(Set.of());
        when(feedMapper.toFeedItem(any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(new CommunityFeedItemResponse(QUESTION_ID, "title", "topic", "author",
                        null, null, 0, 0, false, true, false, "APPROVED", null));

        PaginatedResponse<CommunityFeedItemResponse> result = bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20);

        assertThat(result).isNotNull();
    }

    // COMQL-TC-012: bookmark list hydrates liked correctly per question (2nd caller of toFeedItem)
    @Test
    void getBookmarkedQuestions_someLikedSomeNot_hydratesLikedPerQuestion() {
        UUID question2Id = UUID.randomUUID();
        CommunityQuestion question1 = makeApprovedQuestion();
        CommunityQuestion question2 = CommunityQuestion.builder()
                .id(question2Id).topicId(UUID.randomUUID()).authorId(UUID.randomUUID())
                .title("Q2").body("Q2 body").status(QuestionStatus.APPROVED).anonymous(false).build();
        CommunityBookmark bookmark1 = CommunityBookmark.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(QUESTION_ID).build();
        CommunityBookmark bookmark2 = CommunityBookmark.builder()
                .id(UUID.randomUUID()).userId(USER_ID).questionId(question2Id).build();
        Page<CommunityBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark1, bookmark2));

        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(bookmarkPage);
        when(questionRepository.findAllById(anyCollection())).thenReturn(List.of(question1, question2));
        when(topicRepository.findAllById(any())).thenReturn(List.of());
        when(answerRepository.findQuestionIdsWithExpertAnswer(anyCollection())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(eq(USER_ID), anyCollection())).thenReturn(Set.of(QUESTION_ID));
        when(feedMapper.toFeedItem(eq(question1), any(), any(), anyBoolean(), eq(true), eq(true)))
                .thenReturn(new CommunityFeedItemResponse(QUESTION_ID, "title", "topic", "author",
                        null, null, 0, 0, false, true, true, "APPROVED", null));
        when(feedMapper.toFeedItem(eq(question2), any(), any(), anyBoolean(), eq(true), eq(false)))
                .thenReturn(new CommunityFeedItemResponse(question2Id, "title2", "topic", "author",
                        null, null, 0, 0, false, true, false, "APPROVED", null));

        PaginatedResponse<CommunityFeedItemResponse> result = bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20);

        assertThat(result.getData()).hasSize(2);
        verify(feedMapper).toFeedItem(eq(question1), any(), any(), anyBoolean(), eq(true), eq(true));
        verify(feedMapper).toFeedItem(eq(question2), any(), any(), anyBoolean(), eq(true), eq(false));
        // batch call, not N+1
        verify(likeRepository, org.mockito.Mockito.times(1)).findLikedQuestionIds(eq(USER_ID), anyCollection());
    }

    @Test
    void getBookmarkedQuestions_emptyBookmarks_returnsEmptyPage() {
        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(Page.empty());

        PaginatedResponse<CommunityFeedItemResponse> result = bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20);

        assertThat(result).isNotNull();
    }
}
