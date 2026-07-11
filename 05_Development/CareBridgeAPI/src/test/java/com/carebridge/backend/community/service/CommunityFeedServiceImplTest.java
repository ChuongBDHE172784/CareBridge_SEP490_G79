package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// COM198-TC-001, COM198-TC-004, COM198-TC-006
@ExtendWith(MockitoExtension.class)
class CommunityFeedServiceImplTest {

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private CommunityFeedMapper feedMapper;

    @Mock
    private CommunityTopicRepository topicRepository;

    @Mock
    private CommunityBookmarkRepository bookmarkRepository;

    @Mock
    private CommunityQuestionLikeRepository likeRepository;

    @Mock
    private CommunityAuthorDisplayResolver authorDisplayResolver;

    @InjectMocks
    private CommunityFeedServiceImpl service;

    @BeforeEach
    void setUp() {
        when(authorDisplayResolver.resolveBatch(any())).thenReturn(Map.of());
    }

    private static final UUID TOPIC_A = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID CURRENT_USER = UUID.fromString("00000000-0000-0000-0000-000000000009");

    private CommunityQuestion makeApprovedQuestion(Instant createdAt, boolean isAnonymous) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(TOPIC_A)
                .authorId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .title("Test question " + UUID.randomUUID())
                .body("Test body")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .anonymous(isAnonymous)
                .status(QuestionStatus.APPROVED)
                .build();
    }

    private CommunityFeedItemResponse makeFeedItem(UUID id, Instant createdAt) {
        return new CommunityFeedItemResponse(id, "title", "Thai kỳ", "Author",
                "PREGNANCY", "NORMAL", 0, 0, false, false, false, createdAt);
    }

    // COM198-TC-001: Feed returns visible questions (APPROVED + own PENDING), newest first
    @Test
    void getFeed_noTopicFilter_returnsVisibleNewestFirst() {
        Instant t1 = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant t3 = Instant.now();

        CommunityQuestion q1 = makeApprovedQuestion(t1, false);
        CommunityQuestion q2 = makeApprovedQuestion(t2, false);
        CommunityQuestion q3 = makeApprovedQuestion(t3, false);
        List<CommunityQuestion> questions = List.of(q3, q2, q1);
        Page<CommunityQuestion> pageResult = new PageImpl<>(questions, PageRequest.of(0, 20), 3L);

        when(questionRepository.findFeedVisible(eq(null), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(pageResult);
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(any(), any())).thenReturn(Set.of());
        when(topicRepository.findAllById(any())).thenReturn(List.of(
                com.carebridge.backend.community.entity.CommunityTopic.builder()
                        .id(TOPIC_A).name("Thai kỳ").build()));

        when(feedMapper.toFeedItem(eq(q3), any(), any(), eq(false), eq(false), eq(false))).thenReturn(makeFeedItem(q3.getId(), t3));
        when(feedMapper.toFeedItem(eq(q2), any(), any(), eq(false), eq(false), eq(false))).thenReturn(makeFeedItem(q2.getId(), t2));
        when(feedMapper.toFeedItem(eq(q1), any(), any(), eq(false), eq(false), eq(false))).thenReturn(makeFeedItem(q1.getId(), t1));

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, CURRENT_USER, 0, 20);

        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get(0).createdAt()).isEqualTo(t3);
        assertThat(result.getData().get(2).createdAt()).isEqualTo(t1);
        assertThat(result.getTotalElements()).isEqualTo(3L);
    }

    // COM198-TC-004a: topicId provided → passed through to findFeedVisible
    @Test
    void getFeed_withTopicId_passesTopicIdToRepository() {
        when(questionRepository.findFeedVisible(eq(TOPIC_A), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());

        service.getFeed(TOPIC_A, CURRENT_USER, 0, 20);

        org.mockito.Mockito.verify(questionRepository)
                .findFeedVisible(eq(TOPIC_A), eq(CURRENT_USER), any());
    }

    // COM198-TC-004b: topicId null → passed through as null
    @Test
    void getFeed_withoutTopicId_passesNullTopicId() {
        when(questionRepository.findFeedVisible(eq(null), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());

        service.getFeed(null, CURRENT_USER, 0, 20);

        org.mockito.Mockito.verify(questionRepository)
                .findFeedVisible(eq(null), eq(CURRENT_USER), any());
    }

    // Regression test: the feed must pass the resolved author display name to the mapper instead
    // of hardcoding null (which made every feed item show the generic "Người dùng" fallback).
    @Test
    void getFeed_authorHasDisplayName_passesResolvedNameToMapper() {
        Instant t1 = Instant.now();
        CommunityQuestion q1 = makeApprovedQuestion(t1, false);
        Page<CommunityQuestion> pageResult = new PageImpl<>(List.of(q1), PageRequest.of(0, 20), 1L);

        when(questionRepository.findFeedVisible(eq(null), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(pageResult);
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(any(), any())).thenReturn(Set.of());
        when(topicRepository.findAllById(any())).thenReturn(List.of());
        when(authorDisplayResolver.resolveBatch(any())).thenReturn(Map.of(q1.getAuthorId(), "Nguyễn Thị A"));
        when(feedMapper.toFeedItem(eq(q1), any(), eq("Nguyễn Thị A"), eq(false), eq(false), eq(false)))
                .thenReturn(makeFeedItem(q1.getId(), t1));

        service.getFeed(null, CURRENT_USER, 0, 20);

        org.mockito.Mockito.verify(feedMapper)
                .toFeedItem(eq(q1), any(), eq("Nguyễn Thị A"), eq(false), eq(false), eq(false));
    }

    // COM198-TC-006: Empty results → no exception, empty content
    @Test
    void getFeed_emptyResults_returnsEmptyContent() {
        when(questionRepository.findFeedVisible(any(), any(), any())).thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, CURRENT_USER, 0, 20);

        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // New: PENDING question authored by someone else must not surface for the current viewer —
    // regression guard for the feed-visibility fix (previously leaked to everyone).
    @Test
    void getFeed_repositoryEnforcesPendingAuthorScoping_currentUserIdPassedThrough() {
        when(questionRepository.findFeedVisible(any(), eq(CURRENT_USER), any())).thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(eq(CURRENT_USER), any())).thenReturn(Set.of());

        service.getFeed(null, CURRENT_USER, 0, 20);

        org.mockito.Mockito.verify(questionRepository).findFeedVisible(any(), eq(CURRENT_USER), any());
        org.mockito.Mockito.verify(bookmarkRepository).findBookmarkedQuestionIds(eq(CURRENT_USER), any());
    }

    // COMQL-TC-009: Feed item hydrates liked=true for a question the viewer already liked
    @Test
    void getFeed_questionLikedByCurrentUser_hydratesLikedTrue() {
        Instant t1 = Instant.now();
        CommunityQuestion q1 = makeApprovedQuestion(t1, false);
        Page<CommunityQuestion> pageResult = new PageImpl<>(List.of(q1), PageRequest.of(0, 20), 1L);

        when(questionRepository.findFeedVisible(eq(null), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(pageResult);
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(eq(CURRENT_USER), any())).thenReturn(Set.of(q1.getId()));
        when(topicRepository.findAllById(any())).thenReturn(List.of());
        when(feedMapper.toFeedItem(eq(q1), any(), any(), eq(false), eq(false), eq(true)))
                .thenReturn(new CommunityFeedItemResponse(q1.getId(), "title", "Thai kỳ", "Author",
                        "PREGNANCY", "NORMAL", 0, 0, false, false, true, t1));

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, CURRENT_USER, 0, 20);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).liked()).isTrue();
    }

    // COMQL-TC-010: Feed item hydrates liked=false for a question the viewer has not liked
    @Test
    void getFeed_questionNotLikedByCurrentUser_hydratesLikedFalse() {
        Instant t1 = Instant.now();
        CommunityQuestion q1 = makeApprovedQuestion(t1, false);
        Page<CommunityQuestion> pageResult = new PageImpl<>(List.of(q1), PageRequest.of(0, 20), 1L);

        when(questionRepository.findFeedVisible(eq(null), eq(CURRENT_USER), any(Pageable.class)))
                .thenReturn(pageResult);
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(bookmarkRepository.findBookmarkedQuestionIds(any(), any())).thenReturn(Set.of());
        when(likeRepository.findLikedQuestionIds(eq(CURRENT_USER), any())).thenReturn(Set.of());
        when(topicRepository.findAllById(any())).thenReturn(List.of());
        when(feedMapper.toFeedItem(eq(q1), any(), any(), eq(false), eq(false), eq(false)))
                .thenReturn(makeFeedItem(q1.getId(), t1));

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, CURRENT_USER, 0, 20);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).liked()).isFalse();
    }
}
