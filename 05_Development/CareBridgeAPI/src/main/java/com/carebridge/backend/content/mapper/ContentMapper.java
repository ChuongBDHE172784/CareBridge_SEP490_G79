package com.carebridge.backend.content.mapper;

import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import java.util.List;
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

    // BR-PRIVACY: authorId excluded
    public ContentListResponse toListResponse(ContentItem item) {
        return ContentListResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .stage(item.getStage())
                .topicId(item.getTopicId())
                .publishedAt(item.getPublishedAt())
                .build();
    }

    // BR-PRIVACY: authorId excluded
    public ContentDetailResponse toDetailResponse(ContentItem item) {
        return ContentDetailResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .body(item.getBody())
                .stage(item.getStage())
                .topicId(item.getTopicId())
                .version(item.getVersionNo())
                .publishedAt(item.getPublishedAt())
                .build();
    }

    public ChecklistTemplateResponse toChecklistTemplateResponse(
            ChecklistTemplate template, List<ChecklistItem> items) {
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(this::toChecklistItemResponse)
                .toList();
        return ChecklistTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .stage(template.getStage())
                .description(template.getDescription())
                .items(itemResponses)
                .build();
    }

    private ChecklistItemResponse toChecklistItemResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .itemText(item.getItemText())
                .order(item.getOrder())
                .isRequired(item.getIsRequired())
                .build();
    }
}
