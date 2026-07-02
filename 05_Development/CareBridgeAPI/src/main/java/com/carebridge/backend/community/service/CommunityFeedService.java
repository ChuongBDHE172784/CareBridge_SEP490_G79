package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;

import java.util.UUID;

public interface CommunityFeedService {

    PaginatedResponse<CommunityFeedItemResponse> getFeed(UUID topicId, UUID currentUserId, int page, int size);
}
