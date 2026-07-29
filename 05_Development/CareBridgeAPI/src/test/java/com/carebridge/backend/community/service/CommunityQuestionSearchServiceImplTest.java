package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.request.CommunityQuestionSearchRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// COM162-TC-001, TC-002, TC-004, TC-005, TC-006, TC-008
@ExtendWith(MockitoExtension.class)
class CommunityQuestionSearchServiceImplTest {

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private CommunityTopicRepository topicRepository;

    @Mock
    private CommunityBookmarkRepository bookmarkRepository;

    @Mock
    private CommunityQuestionLikeRepository likeRepository;

    @Mock
    private CommunityAuthorDisplayResolver authorDisplayResolver;

    @Spy
    private CommunityQuestionMapper questionMapper;

    @InjectMocks
    private CommunityQuestionSearchServiceImpl service;

    private CommunityQuestionSearchRequest makeSearchRequest() {
        CommunityQuestionSearchRequest req = new CommunityQuestionSearchRequest();
        req.setPage(0);
        req.setSize(20);
        return req;
    }

    private CommunityQuestionSearchRequest makeSearchRequest(Consumer<CommunityQuestionSearchRequest> overrides) {
        CommunityQuestionSearchRequest req = makeSearchRequest();
        overrides.accept(req);
        return req;
    }

    private CommunityQuestion makeApprovedQuestion(String title) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(UUID.randomUUID())
                .title(title)
                .body("Some body text")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .answerCount(0)
                .build();
    }

    private void stubEmptyDependencies() {
        when(topicRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());
    }

    // COM162-TC-001: keyword search returns matching APPROVED questions
    @Test
    void searchQuestions_withKeyword_returnsMatchingApproved() {
        CommunityQuestion q1 = makeApprovedQuestion("Thai nhi 28 tuần đạp");
        CommunityQuestion q2 = makeApprovedQuestion("Thai nhi 20 tuần phát triển");
        Page<CommunityQuestion> page = new PageImpl<>(List.of(q1, q2), PageRequest.of(0, 20), 2L);

        when(questionRepository.searchApproved(
                eq("thai nhi"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        stubEmptyDependencies();

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> r.setKeyword("thai nhi")));

        assertThat(result.getData()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getPage()).isZero();
    }

    // COM162-TC-002: no results → PaginatedResponse with empty content (not exception)
    @Test
    void searchQuestions_noResults_returnsEmptyContent() {
        when(questionRepository.searchApproved(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of());

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> r.setKeyword("xyz_nonexistent")));

        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // COM162-TC-004: topicId filter is forwarded to repository
    @Test
    void searchQuestions_topicIdFilter_forwardsTopicIdToRepository() {
        UUID topicA = UUID.randomUUID();
        CommunityQuestion q = makeApprovedQuestion("Topic A question");
        q.setTopicId(topicA);

        Page<CommunityQuestion> filtered = new PageImpl<>(List.of(q), PageRequest.of(0, 20), 1L);
        when(questionRepository.searchApproved(isNull(), eq(topicA), isNull(), isNull(), any()))
                .thenReturn(filtered);
        stubEmptyDependencies();

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> r.setTopicId(topicA)));

        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    // COM162-TC-005: stage filter is forwarded to repository
    @Test
    void searchQuestions_stageFilter_forwardsStageToRepository() {
        when(questionRepository.searchApproved(isNull(), isNull(), eq(PregnancyStage.PREGNANCY), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        makeApprovedQuestion("P1"), makeApprovedQuestion("P2"), makeApprovedQuestion("P3")
                ), PageRequest.of(0, 20), 3L));
        stubEmptyDependencies();

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> r.setStage(PregnancyStage.PREGNANCY)));

        assertThat(result.getTotalElements()).isEqualTo(3L);
    }

    // COM162-TC-006: hasExpertAnswer=true → response items have hasExpertAnswer=true
    @Test
    void searchQuestions_hasExpertAnswerTrue_responseItemsHaveExpertAnswerTrue() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();

        CommunityQuestion q1 = CommunityQuestion.builder()
                .id(q1Id).topicId(topicId).title("Expert answered Q1")
                .body("body").stage(PregnancyStage.PREGNANCY).urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED).answerCount(1).build();
        CommunityQuestion q2 = CommunityQuestion.builder()
                .id(q2Id).topicId(topicId).title("Expert answered Q2")
                .body("body").stage(PregnancyStage.PREGNANCY).urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED).answerCount(2).build();

        when(questionRepository.searchApproved(isNull(), isNull(), isNull(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(q1, q2), PageRequest.of(0, 20), 2L));
        when(topicRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(answerRepository.findQuestionIdsWithExpertAnswer(any())).thenReturn(Set.of(q1Id, q2Id));

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> r.setHasExpertAnswer(true)));

        assertThat(result.getTotalElements()).isEqualTo(2L);
        result.getData().forEach(q -> assertThat(q.hasExpertAnswer()).isTrue());
    }

    // COM162-TC-008: page=1 is forwarded, response reflects correct pagination metadata
    // Note: PageImpl adjusts total when offset+pageSize>total → need content.size() consistent with total
    // page=1, size=20, total=25 → 5 items on last page (20 + 5 = 25 matches total)
    @Test
    void searchQuestions_page1_returnsSecondPageMetadata() {
        Page<CommunityQuestion> secondPage = new PageImpl<>(
                List.of(
                        makeApprovedQuestion("P1"), makeApprovedQuestion("P2"),
                        makeApprovedQuestion("P3"), makeApprovedQuestion("P4"),
                        makeApprovedQuestion("P5")
                ),
                PageRequest.of(1, 20), 25L
        );
        when(questionRepository.searchApproved(any(), any(), any(), any(),
                argThat(p -> p.getPageNumber() == 1))).thenReturn(secondPage);
        stubEmptyDependencies();

        PaginatedResponse<CommunityQuestionSummaryResponse> result =
                service.searchQuestions(makeSearchRequest(r -> {
                    r.setPage(1);
                    r.setSize(20);
                }));

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(25L);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
