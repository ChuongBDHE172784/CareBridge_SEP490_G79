package com.carebridge.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.entity.SearchType;
import com.carebridge.backend.search.provider.QuestionSearchProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * SEARCH-TC-013-006 / SEARCH-TC-013-007 (Test-Spec §4) — visibility filter and snippet
 * generation. Reuses {@link CommunityQuestionRepository#searchApproved}, which already
 * excludes DELETED/PENDING/HIDDEN/LOCKED questions (UC-162) — this test verifies the
 * provider maps that repository's output correctly, not the SQL itself.
 */
@ExtendWith(MockitoExtension.class)
class QuestionSearchProviderTest {

    @Mock
    private CommunityQuestionRepository questionRepository;

    private CommunityQuestion makeQuestion(String title, String body) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title(title)
                .body(body)
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void search_supportsOnlyQuestionType() {
        QuestionSearchProvider provider = new QuestionSearchProvider(questionRepository);

        assertThat(provider.supports(SearchType.QUESTION)).isTrue();
        assertThat(provider.supports(SearchType.CONTENT)).isFalse();
    }

    // SEARCH-TC-013-006: only APPROVED questions returned (delegates to searchApproved which
    // already excludes DELETED/PENDING/HIDDEN/LOCKED per UC-162 query — TC-COND-006).
    @Test
    void search_returnsOnlyApprovedQuestions_mappedToSearchItems() {
        QuestionSearchProvider provider = new QuestionSearchProvider(questionRepository);
        // Pageable size must be >= declared total, otherwise Spring's PageImpl constructor
        // recalculates total as (offset + content.size()) — use all 5 items on one page.
        Pageable pageable = PageRequest.of(0, 20);
        List<CommunityQuestion> approved = List.of(
                makeQuestion("Câu hỏi về thai kỳ 1", "Nội dung 1"),
                makeQuestion("Câu hỏi về thai kỳ 2", "Nội dung 2"),
                makeQuestion("Câu hỏi về thai kỳ 3", "Nội dung 3"),
                makeQuestion("Câu hỏi về thai kỳ 4", "Nội dung 4"),
                makeQuestion("Câu hỏi về thai kỳ 5", "Nội dung 5"));
        Page<CommunityQuestion> page = new PageImpl<>(approved, pageable, 5);

        when(questionRepository.searchApproved(eq("thai kỳ"), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        Page<SearchItemResponse> result = provider.search("thai kỳ", UUID.randomUUID(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent().get(0).getType()).isEqualTo(SearchType.QUESTION);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Câu hỏi về thai kỳ 1");
    }

    // SEARCH-TC-013-007: snippet <= 200 chars, HTML stripped
    @Test
    void search_snippetIsStrippedOfHtmlAndTruncatedTo200Chars() {
        QuestionSearchProvider provider = new QuestionSearchProvider(questionRepository);
        Pageable pageable = PageRequest.of(0, 20);
        String longHtmlBody = "Mình đang <b>mang thai</b> tuần 28. ".repeat(20);
        CommunityQuestion question = makeQuestion("Title", longHtmlBody);
        Page<CommunityQuestion> page = new PageImpl<>(List.of(question), pageable, 1);

        when(questionRepository.searchApproved(any(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        Page<SearchItemResponse> result = provider.search("mang thai", UUID.randomUUID(), pageable);

        String snippet = result.getContent().get(0).getSnippet();
        assertThat(snippet.length()).isLessThanOrEqualTo(200);
        assertThat(snippet).doesNotContain("<").doesNotContain(">");
    }
}
