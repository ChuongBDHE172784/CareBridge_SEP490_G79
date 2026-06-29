package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import java.util.List;
import java.util.UUID;

public interface CommunityTopicService {

    List<CommunityTopicResponse> getTopics(boolean includeHidden);

    List<CommunityTopicResponse> searchTopics(String keyword, boolean includeHidden);

    CommunityTopicResponse createTopic(java.util.UUID createdBy, CreateCommunityTopicRequest request);

    CommunityTopicResponse updateTopic(UUID id, UpdateCommunityTopicRequest request);
}
