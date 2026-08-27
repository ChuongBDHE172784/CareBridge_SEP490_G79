package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.TopicFollowResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.UserTopicFollow;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.TopicHiddenException;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicFollowServiceImpl implements TopicFollowService {

    private final UserTopicFollowRepository followRepository;
    private final CommunityTopicRepository topicRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public TopicFollowResponse toggleFollow(UUID topicId, UUID userId) {
        CommunityTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CommunityTopicNotFoundException(topicId.toString()));

        if (topic.isHidden()) {
            throw new TopicHiddenException(topicId.toString());
        }

        Optional<UserTopicFollow> existing = followRepository.findByUserIdAndTopicId(userId, topicId);

        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            auditService.log(AuditAction.COMMUNITY_PROFILE_UPDATED, userId,
                    "CommunityTopicFollow", topicId.toString(), "unfollowed");
            return TopicFollowResponse.builder().topicId(topicId).followed(false).build();
        }

        followRepository.save(UserTopicFollow.builder().userId(userId).topicId(topicId).build());
        auditService.log(AuditAction.COMMUNITY_PROFILE_UPDATED, userId,
                "CommunityTopicFollow", topicId.toString(), "followed");
        return TopicFollowResponse.builder().topicId(topicId).followed(true).build();
    }
}
