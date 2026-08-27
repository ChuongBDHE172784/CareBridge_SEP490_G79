package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.response.TopicFollowResponse;

import java.util.UUID;

public interface TopicFollowService {

    /**
     * Toggles follow state on a community topic for the given user.
     * @throws com.carebridge.backend.community.exception.CommunityTopicNotFoundException (COM-003) when topic does not exist
     * @throws com.carebridge.backend.community.exception.TopicHiddenException (COM-014) when topic is hidden
     */
    TopicFollowResponse toggleFollow(UUID topicId, UUID userId);
}
