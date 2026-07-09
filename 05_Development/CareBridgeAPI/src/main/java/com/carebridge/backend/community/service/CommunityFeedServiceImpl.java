package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
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
public class CommunityFeedServiceImpl implements CommunityFeedService {

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityBookmarkRepository bookmarkRepository;
    private final CommunityQuestionLikeRepository likeRepository;
    private final CommunityProfileRepository profileRepository;
    private final com.carebridge.backend.profile.repository.ProfileRepository userProfileRepository;
    private final CommunityFeedMapper feedMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CommunityFeedItemResponse> getFeed(UUID topicId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // UC-198 (fixed): APPROVED visible to everyone, PENDING visible only to its own author —
        // previously every user's PENDING question leaked to the whole feed (see repository note).
        Page<CommunityQuestion> questions = questionRepository.findFeedVisible(topicId, currentUserId, pageable);

        // Batch fetch topic names to avoid N+1
        Set<UUID> topicIds = questions.stream()
                .map(CommunityQuestion::getTopicId)
                .collect(Collectors.toSet());
        Map<UUID, String> topicNames = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(
                        t -> t.getId(),
                        t -> t.getName()));

        // Batch fetch question author profiles to avoid N+1
        Set<UUID> authorIds = questions.stream()
                .map(CommunityQuestion::getAuthorId)
                .collect(Collectors.toSet());
        Map<UUID, String> authorDisplayNames = profileRepository.findAllByUserIdIn(authorIds).stream()
                .collect(Collectors.toMap(
                        CommunityProfile::getUserId,
                        CommunityProfile::getDisplayName,
                        (existing, replacement) -> existing
                ));

        // Fallback to UserProfile.displayName if CommunityProfile not found or name blank
        Set<UUID> missingAuthorIds = authorIds.stream()
                .filter(id -> id != null && (!authorDisplayNames.containsKey(id) || authorDisplayNames.get(id) == null || authorDisplayNames.get(id).isBlank()))
                .collect(Collectors.toSet());
        if (!missingAuthorIds.isEmpty()) {
            userProfileRepository.findAllByUserIdIn(missingAuthorIds).forEach(up -> {
                if (up.getDisplayName() != null && !up.getDisplayName().isBlank()) {
                    authorDisplayNames.put(up.getUserId(), up.getDisplayName());
                }
            });
        }

        // Batch check expert answers to avoid N+1
        List<UUID> questionIds = questions.stream()
                .map(CommunityQuestion::getId)
                .collect(Collectors.toList());
        Set<UUID> expertAnsweredIds = answerRepository.findQuestionIdsWithExpertAnswer(questionIds);

        // Batch check bookmark state to avoid N+1 (UC-58 hydration fix)
        Set<UUID> bookmarkedIds = bookmarkRepository.findBookmarkedQuestionIds(currentUserId, questionIds);

        // Batch check like state to avoid N+1
        Set<UUID> likedIds = likeRepository.findLikedQuestionIds(currentUserId, questionIds);

        Page<CommunityFeedItemResponse> feedPage = questions.map(q -> {
            String tName = topicNames.getOrDefault(q.getTopicId(), "");
            boolean hasExpert = expertAnsweredIds.contains(q.getId());
            boolean isBookmarked = bookmarkedIds.contains(q.getId());
            boolean isLiked = likedIds.contains(q.getId());
            String authorDisplay = authorDisplayNames.get(q.getAuthorId());
            return feedMapper.toFeedItem(q, tName, authorDisplay, hasExpert, isBookmarked, isLiked);
        });

        return PaginatedResponse.of(feedPage);
    }
}
