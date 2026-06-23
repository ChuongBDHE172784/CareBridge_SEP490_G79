package com.carebridge.backend.content.mapper;

import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {

    public ContentItem toEntity(CreateContentRequest request, Long authorUserId) {
        return ContentItem.builder()
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .stage(request.getStage())
                .topicId(request.getTopicId())
                .status(ContentStatus.DRAFT)
                .versionNo(1)
                .authorUserId(authorUserId)
                .build();
    }

    public CreateContentResponse toCreateResponse(ContentItem entity) {
        return CreateContentResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .stage(entity.getStage())
                .status(entity.getStatus().name())
                .version(entity.getVersionNo())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
