package com.carebridge.backend.content.mapper;

import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.ReviewFeedbackResponse;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentSource;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {

    public ContentItem toEntity(CreateContentRequest request, java.util.UUID authorUserId) {
        return ContentItem.builder()
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .stage(request.getStage())
                .topicId(request.getTopicId())
                .status(ContentStatus.DRAFT)
                .versionNo(1)
                .authorUserId(authorUserId)
                .sources(request.getSources() == null ? new ArrayList<>() : request.getSources().stream()
                        .map(s -> new ContentSource(s.title(), s.url(), s.publisher()))
                        .collect(Collectors.toCollection(ArrayList::new)))
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
        Instant updatedAt = item.getUpdatedAt();
        boolean contentStale = updatedAt != null
                && updatedAt.isBefore(Instant.now().minus(365, ChronoUnit.DAYS));
        return ContentDetailResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .body(item.getBody())
                .stage(item.getStage())
                .topicId(item.getTopicId())
                .version(item.getVersionNo())
                .status(item.getStatus())
                .sourceLabel(item.getSourceLabel())
                .publishedAt(item.getPublishedAt())
                .updatedAt(updatedAt)
                .createdAt(item.getCreatedAt())
                .sources(item.getSources() == null ? List.of() : item.getSources().stream()
                        .map(s -> new com.carebridge.backend.content.dto.response.ContentSourceResponse(
                                s.getTitle(), s.getUrl(), s.getPublisher()))
                        .toList())
                .contentStale(contentStale)
                .build();
    }

    public StaffContentDetailResponse toStaffDetailResponse(ContentItem item) {
        return new StaffContentDetailResponse(
                toDetailResponse(item),
                toReviewFeedback(
                        item.getRevisionReason(), item.getRevisionRequestedAt(),
                        item.getRevisionRequestedBy(), item.getRevisionRequestedVersion()));
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

    public AdminChecklistTemplateDetailResponse toAdminChecklistTemplateDetailResponse(
            ChecklistTemplate template, List<ChecklistItem> items) {
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(this::toChecklistItemResponse)
                .toList();
        return AdminChecklistTemplateDetailResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .stage(template.getStage())
                .status(template.getStatus())
                .description(template.getDescription())
                .versionNo(template.getVersionNo())
                .items(itemResponses)
                .latestReviewFeedback(toReviewFeedback(
                        template.getRevisionReason(), template.getRevisionRequestedAt(),
                        template.getRevisionRequestedBy(), template.getRevisionRequestedVersion()))
                .build();
    }

    // BR-PRIVACY: authorId excluded; topicName null for MVP (C5 — deferred cross-module resolution)
    public ContentSearchResponse toSearchResponse(ContentItem item) {
        return ContentSearchResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .stage(item.getStage())
                .topicName(null)
                .publishedAt(item.getPublishedAt())
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

    public ReviewFeedbackResponse toReviewFeedback(
            String reason, Instant requestedAt, java.util.UUID requestedBy, Integer versionNo) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return new ReviewFeedbackResponse(reason, requestedAt, requestedBy, versionNo);
    }
}
