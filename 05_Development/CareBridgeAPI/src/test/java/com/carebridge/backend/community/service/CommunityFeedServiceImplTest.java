package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
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

    @InjectMocks
    private CommunityFeedServiceImpl service;

    private static final UUID TOPIC_A = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID TOPIC_B = UUID.fromString("00000000-0000-0000-0002-000000000002");

    private CommunityQuestion makeApprovedQuestion(Instant createdAt, boolean isAnonymous) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(TOPIC_A)
                .authorId(1L)
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
                "PREGNANCY", "NORMAL", 0, 0, false, createdAt);
    }

    // COM198-TC-001: Feed returns APPROVED questions, newest first
    @Test
    void getFeed_noTopicFilter_returnsApprovedNewestFirst() {
        Instant t1 = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant t3 = Instant.now();

        CommunityQuestion q1 = makeApprovedQuestion(t1, false);
        CommunityQuestion q2 = makeApprovedQuestion(t2, false);
        CommunityQuestion q3 = makeApprovedQuestion(t3, false);
        List<CommunityQuestion> questions = List.of(q3, q2, q1);
        Page<CommunityQuestion> pageResult = new PageImpl<>(questions, PageRequest.of(0, 20), 3L);

        when(questionRepository.findAllByStatusOrderByCreatedAtDesc(
                eq(QuestionStatus.APPROVED), any(Pageable.class))).thenReturn(pageResult);
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
        when(topicRepository.findAllById(any())).thenReturn(List.of(
                com.carebridge.backend.community.entity.CommunityTopic.builder()
                        .id(TOPIC_A).name("Thai kỳ").build()));

        when(feedMapper.toFeedItem(eq(q3), any(), any(), eq(false))).thenReturn(makeFeedItem(q3.getId(), t3));
        when(feedMapper.toFeedItem(eq(q2), any(), any(), eq(false))).thenReturn(makeFeedItem(q2.getId(), t2));
        when(feedMapper.toFeedItem(eq(q1), any(), any(), eq(false))).thenReturn(makeFeedItem(q1.getId(), t1));

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, 0, 20);

        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get(0).createdAt()).isEqualTo(t3);
        assertThat(result.getData().get(2).createdAt()).isEqualTo(t1);
        assertThat(result.getTotalElements()).isEqualTo(3L);
    }

    // COM198-TC-004a: topicId provided → calls topic-filtered method
    @Test
    void getFeed_withTopicId_callsTopicFilteredMethod() {
        when(questionRepository.findAllByStatusAndTopicIdOrderByCreatedAtDesc(
                eq(QuestionStatus.APPROVED), eq(TOPIC_A), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());

        service.getFeed(TOPIC_A, 0, 20);

        verify(questionRepository).findAllByStatusAndTopicIdOrderByCreatedAtDesc(
                eq(QuestionStatus.APPROVED), eq(TOPIC_A), any());
        verify(questionRepository, never()).findAllByStatusOrderByCreatedAtDesc(any(), any());
    }

    // COM198-TC-004b: topicId null → calls all-topics method
    @Test
    void getFeed_withoutTopicId_callsAllTopicsMethod() {
        when(questionRepository.findAllByStatusOrderByCreatedAtDesc(
                eq(QuestionStatus.APPROVED), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());

        service.getFeed(null, 0, 20);

        verify(questionRepository).findAllByStatusOrderByCreatedAtDesc(any(), any());
        verify(questionRepository, never())
                .findAllByStatusAndTopicIdOrderByCreatedAtDesc(any(), any(), any());
    }

    // COM198-TC-006: Empty results → no exception, empty content
    @Test
    void getFeed_emptyResults_returnsEmptyContent() {
        when(questionRepository.findAllByStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());

        PaginatedResponse<CommunityFeedItemResponse> result = service.getFeed(null, 0, 20);

        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
