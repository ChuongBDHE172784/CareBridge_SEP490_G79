package com.carebridge.backend.search.provider;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.entity.SearchType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * UC-13 search provider for community questions.
 *
 * <p>Reuses {@link CommunityQuestionRepository#searchApproved} (built for UC-162) which
 * already excludes DELETED/PENDING/HIDDEN/LOCKED questions (status = APPROVED only) and
 * uses bind params (no string concatenation — OWASP A03 safe). Not reinventing that query.
 */
@Component
@RequiredArgsConstructor
public class QuestionSearchProvider implements DomainSearchProvider {

    private static final int SNIPPET_MAX_LENGTH = 200;

    private final CommunityQuestionRepository questionRepository;

    @Override
    public boolean supports(SearchType type) {
        return type == SearchType.QUESTION;
    }

    @Override
    public Page<SearchItemResponse> search(String q, UUID userId, Pageable pageable) {
        Page<CommunityQuestion> page = questionRepository.searchApproved(q, null, null, null, null, pageable);
        return page.map(this::toItem);
    }

    private SearchItemResponse toItem(CommunityQuestion question) {
        return SearchItemResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .snippet(snippet(question.getBody()))
                .type(SearchType.QUESTION)
                .metadata(Map.of("status", question.getStatus().name()))
                .createdAt(question.getCreatedAt())
                .build();
    }

    private String snippet(String body) {
        if (body == null) {
            return "";
        }
        String stripped = body.replaceAll("<[^>]*>", "");
        return stripped.length() > SNIPPET_MAX_LENGTH ? stripped.substring(0, SNIPPET_MAX_LENGTH) : stripped;
    }
}
