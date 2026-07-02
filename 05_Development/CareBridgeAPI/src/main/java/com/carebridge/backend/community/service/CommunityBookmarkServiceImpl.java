package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.BookmarkToggleResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityBookmark;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
public class CommunityBookmarkServiceImpl implements CommunityBookmarkService {

    private final CommunityBookmarkRepository bookmarkRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityAnswerRepository answerRepository;
    private final CommunityFeedMapper feedMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public BookmarkToggleResponse toggleBookmark(UUID userId, UUID questionId) {
        questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

        if (bookmarkRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            CommunityBookmark bookmark = bookmarkRepository
                    .findByUserIdAndQuestionId(userId, questionId)
                    .orElseThrow();
            bookmarkRepository.delete(bookmark);
            auditService.log(AuditAction.COMMUNITY_BOOKMARK_TOGGLED, userId,
                    "CommunityBookmark", questionId.toString(), "removed");
            return BookmarkToggleResponse.builder()
                    .bookmarked(false)
                    .questionId(questionId)
                    .build();
        } else {
            CommunityBookmark bookmark = CommunityBookmark.builder()
                    .userId(userId)
                    .questionId(questionId)
                    .build();
            bookmarkRepository.save(bookmark);
            auditService.log(AuditAction.COMMUNITY_BOOKMARK_TOGGLED, userId,
                    "CommunityBookmark", questionId.toString(), "added");
            return BookmarkToggleResponse.builder()
                    .bookmarked(true)
                    .questionId(questionId)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CommunityFeedItemResponse> getBookmarkedQuestions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommunityBookmark> bookmarkPage = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        if (bookmarkPage.isEmpty()) {
            return PaginatedResponse.of(Page.empty(pageable));
        }

        // Collect question IDs
        List<UUID> questionIds = bookmarkPage.stream()
                .map(CommunityBookmark::getQuestionId)
                .collect(Collectors.toList());

        // Batch-load questions
        Map<UUID, CommunityQuestion> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(CommunityQuestion::getId, q -> q));

        // Batch-load topic names
        Set<UUID> topicIds = questionMap.values().stream()
                .map(CommunityQuestion::getTopicId)
                .collect(Collectors.toSet());
        Map<UUID, String> topicNames = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(CommunityTopic::getId, CommunityTopic::getName));

        // Batch check expert answers
        Set<UUID> expertAnsweredIds = answerRepository.findQuestionIdsWithExpertAnswer(questionIds);

        // Map bookmarks to feed items (skip if question not found or not APPROVED)
        List<CommunityFeedItemResponse> items = bookmarkPage.stream()
                .map(b -> questionMap.get(b.getQuestionId()))
                .filter(q -> q != null && q.getStatus() == QuestionStatus.APPROVED)
                .map(q -> {
                    String topicName = topicNames.getOrDefault(q.getTopicId(), "");
                    boolean hasExpert = expertAnsweredIds.contains(q.getId());
                    // Every item here is, by construction, bookmarked by this user.
                    return feedMapper.toFeedItem(q, topicName, null, hasExpert, true);
                })
                .collect(Collectors.toList());

        Page<CommunityFeedItemResponse> feedPage = new PageImpl<>(items, pageable, bookmarkPage.getTotalElements());
        return PaginatedResponse.of(feedPage);
    }
}
