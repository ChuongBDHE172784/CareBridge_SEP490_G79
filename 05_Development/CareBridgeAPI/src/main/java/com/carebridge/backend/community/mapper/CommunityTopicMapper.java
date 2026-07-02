package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import org.springframework.stereotype.Component;

@Component
public class CommunityTopicMapper {

    public CommunityTopic toEntity(CreateCommunityTopicRequest request, java.util.UUID createdBy) {
        return CommunityTopic.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder())
                .createdBy(createdBy)
                .isHidden(false)
                .build();
    }

    public CommunityTopicResponse toResponse(CommunityTopic entity) {
        return toResponse(entity, false);
    }

    // UC-171 hydration fix: "isFollowed" reflects the CURRENT viewer's follow state, computed by
    // the caller (batch per-user lookup) — never derivable from the entity alone.
    public CommunityTopicResponse toResponse(CommunityTopic entity, boolean isFollowed) {
        return CommunityTopicResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .isHidden(entity.isHidden())
                .isFollowed(isFollowed)
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void applyUpdate(CommunityTopic entity, UpdateCommunityTopicRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }
        if (request.getIsHidden() != null) {
            entity.setHidden(request.getIsHidden());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
    }
}
