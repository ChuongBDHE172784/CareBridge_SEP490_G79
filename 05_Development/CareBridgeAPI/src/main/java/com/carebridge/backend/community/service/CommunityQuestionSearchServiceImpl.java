package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.request.CommunityQuestionSearchRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityQuestionSearchServiceImpl implements CommunityQuestionSearchService {

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityBookmarkRepository bookmarkRepository;
    private final CommunityQuestionLikeRepository likeRepository;
    private final CommunityQuestionMapper questionMapper;
    private final CommunityAuthorDisplayResolver authorDisplayResolver;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CommunityQuestionSummaryResponse> searchQuestions(
            CommunityQuestionSearchRequest request) {
        return searchQuestions(request, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CommunityQuestionSummaryResponse> searchQuestions(
            CommunityQuestionSearchRequest request, UUID currentUserId) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<CommunityQuestion> questions = questionRepository.searchApproved(
                request.getKeyword(),
                request.getTopicId(),
                request.getStage(),
                request.getUrgency(),
                request.getHasExpertAnswer(),
                pageable);

        // Batch-fetch topic names to avoid N+1
        Set<UUID> topicIds = questions.stream()
                .map(CommunityQuestion::getTopicId)
                .collect(Collectors.toSet());
        Map<UUID, String> topicNames = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(
                        t -> t.getId(),
                        t -> t.getName()));

        // Batch-check expert answers to avoid N+1
        List<UUID> questionIds = questions.stream()
                .map(CommunityQuestion::getId)
                .collect(Collectors.toList());
        Set<UUID> expertAnsweredIds = answerRepository.findQuestionIdsWithExpertAnswer(questionIds);

        // Batch check bookmark state to avoid N+1
        Set<UUID> bookmarkedIds = (currentUserId != null && !questionIds.isEmpty())
                ? bookmarkRepository.findBookmarkedQuestionIds(currentUserId, questionIds)
                : Set.of();

        // Batch check like state to avoid N+1
        Set<UUID> likedIds = (currentUserId != null && !questionIds.isEmpty())
                ? likeRepository.findLikedQuestionIds(currentUserId, questionIds)
                : Set.of();

        // Batch fetch question author display names to avoid N+1
        Set<UUID> authorIds = questions.stream().map(CommunityQuestion::getAuthorId).collect(Collectors.toSet());
        Map<UUID, String> authorDisplayNames = authorDisplayResolver.resolveBatch(authorIds);

        Page<CommunityQuestionSummaryResponse> responsePage = questions.map(q -> {
            String topicName = topicNames.getOrDefault(q.getTopicId(), "");
            boolean hasExpert = expertAnsweredIds.contains(q.getId());
            boolean isBookmarked = bookmarkedIds.contains(q.getId());
            boolean isLiked = likedIds.contains(q.getId());
            String authorDisplay = authorDisplayNames.get(q.getAuthorId());
            return questionMapper.toSummaryResponse(q, topicName, authorDisplay, hasExpert, isBookmarked, isLiked);
        });

        return PaginatedResponse.of(responsePage);
    }
}
