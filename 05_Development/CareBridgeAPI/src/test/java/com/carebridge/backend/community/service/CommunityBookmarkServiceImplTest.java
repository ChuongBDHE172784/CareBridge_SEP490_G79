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
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
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
    @Mock CommunityFeedMapper feedMapper;
    @Mock AuditService auditService;
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
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeApprovedQuestion()));
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
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeApprovedQuestion()));
        when(bookmarkRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(true);
        when(bookmarkRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)).thenReturn(Optional.of(existing));

        BookmarkToggleResponse result = bookmarkService.toggleBookmark(USER_ID, QUESTION_ID);

        assertThat(result.isBookmarked()).isFalse();
        verify(bookmarkRepository).delete(existing);
    }

    @Test
    void toggleBookmark_questionNotFound_throws() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

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
        when(feedMapper.toFeedItem(any(), any(), any(), anyBoolean()))
                .thenReturn(new CommunityFeedItemResponse(QUESTION_ID, "title", "topic", "author",
                        null, null, 0, 0, false, null));

        PaginatedResponse<CommunityFeedItemResponse> result = bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20);

        assertThat(result).isNotNull();
    }

    @Test
    void getBookmarkedQuestions_emptyBookmarks_returnsEmptyPage() {
        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(Page.empty());

        PaginatedResponse<CommunityFeedItemResponse> result = bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20);

        assertThat(result).isNotNull();
    }
}
