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
                .type(request.getType())
                .parentId(request.getParentId())
                .sortOrder(request.getSortOrder())
                .createdBy(createdBy)
                .isHidden(false)
                .build();
        // Note: slug is intentionally NOT set here — CommunityTopicServiceImpl computes and
        // assigns it (collision-checked via SlugGenerator + repository) before save() (ADR-COM-018).
    }

    public CommunityTopicResponse toResponse(CommunityTopic entity) {
        return toResponse(entity, false);
    }

    // UC-171 hydration fix: "isFollowed" reflects the CURRENT viewer's follow state, computed by
    // the caller (batch per-user lookup) — never derivable from the entity alone.
    // questionCount is intentionally NOT set here — the caller layers it on via
    // response.toBuilder().questionCount(...).build() (ADR-COM-015), so this method's mocked
    // call sites in existing tests keep working unchanged.
    public CommunityTopicResponse toResponse(CommunityTopic entity, boolean isFollowed) {
        return CommunityTopicResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .type(entity.getType())
                .slug(entity.getSlug())
                .parentId(entity.getParentId())
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
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getParentId() != null) {
            entity.setParentId(request.getParentId());
        }
        if (request.getIsHidden() != null) {
            entity.setHidden(request.getIsHidden());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
    }
}
