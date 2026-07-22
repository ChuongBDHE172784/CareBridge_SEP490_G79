package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.TopicType;
import java.util.List;
import java.util.UUID;

public interface CommunityTopicService {

    // type == null -> all types (web management); type != null -> filtered (ADR-COM-017).
    List<CommunityTopicResponse> getTopics(boolean includeHidden, TopicType type, UUID currentUserId);

    List<CommunityTopicResponse> searchTopics(String keyword, boolean includeHidden, TopicType type, UUID currentUserId);

    /**
     * @throws com.carebridge.backend.community.exception.DuplicateTopicNameException (COM-009) khi tên trùng
     * @throws com.carebridge.backend.community.exception.InvalidTopicHierarchyException (COM-015) khi vi phạm ADR-COM-016
     * @throws com.carebridge.backend.community.exception.CommunityTopicNotFoundException (COM-003) khi parentId không tồn tại/đang ẩn
     */
    CommunityTopicResponse createTopic(java.util.UUID createdBy, CreateCommunityTopicRequest request);

    /**
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException khi topic không tồn tại
     * @throws com.carebridge.backend.community.exception.DuplicateTopicNameException (COM-009) khi tên trùng
     * @throws com.carebridge.backend.community.exception.InvalidTopicHierarchyException (COM-015) khi vi phạm ADR-COM-016
     */
    CommunityTopicResponse updateTopic(UUID id, UUID updatedBy, UpdateCommunityTopicRequest request);
}
