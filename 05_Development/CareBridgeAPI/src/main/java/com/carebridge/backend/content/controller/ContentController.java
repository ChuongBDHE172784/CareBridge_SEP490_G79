package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.dto.response.LifecycleContentEnvelope;
import com.carebridge.backend.common.util.SecurityUtils;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ContentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ContentListResponse>> getContents(
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 50) {
            throw ContentException.validationFailed("size", "must not exceed 50");
        }

        ContentFilterRequest filter = ContentFilterRequest.builder()
                .type(type)
                .stage(stage)
                .topicId(topicId)
                .build();

        PageRequest pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<ContentListResponse> result = contentService.getContents(filter, pageable);
        return ResponseEntity.ok(PaginatedResponse.of(result));
    }

    // UC-224: Search verified content by keyword, stage, topicId, type (CB-CONTENT-IMP-002)
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ContentSearchResponse>> searchContent(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (keyword == null || keyword.isBlank()) {
            throw ContentException.validationFailed("keyword", "is required");
        }
        if (keyword.length() > 100) {
            throw ContentException.validationFailed("keyword", "max length is 100");
        }
        if (size > 50) {
            throw ContentException.validationFailed("size", "must not exceed 50");
        }

        ContentSearchRequest request = new ContentSearchRequest();
        request.setKeyword(keyword);
        request.setType(type);
        request.setStage(stage);
        request.setTopicId(topicId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<ContentSearchResponse> result = contentService.searchContent(request, pageable);
        return ResponseEntity.ok(PaginatedResponse.of(result));
    }

    @GetMapping("/checklists")
    public ResponseEntity<ApiResponse<List<ChecklistTemplateResponse>>> getChecklists(
            @RequestParam(required = false) ContentStage stage) {
        List<ChecklistTemplateResponse> result = contentService.getChecklists(stage);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/lifecycle")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<LifecycleContentEnvelope<PaginatedResponse<ContentListResponse>>>>
            getLifecycleContents(
                    @RequestParam(required = false) ContentType type,
                    @RequestParam(required = false) UUID topicId,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size,
                    Principal principal) {
        validatePage(page, size);
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        var result = contentService.getLifecycleContents(
                callerId, type, topicId, PageRequest.of(page, size, Sort.by("publishedAt").descending()));
        var envelope = new LifecycleContentEnvelope<>(result.stage(), PaginatedResponse.of(result.payload()));
        return ResponseEntity.ok(ApiResponse.success(envelope));
    }

    @GetMapping("/lifecycle/checklists")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<LifecycleContentEnvelope<List<ChecklistTemplateResponse>>>>
            getLifecycleChecklists(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getLifecycleChecklists(
                SecurityUtils.requireCurrentUserId(principal))));
    }

    @GetMapping("/lifecycle/{id}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<LifecycleContentEnvelope<ContentDetailResponse>>>
            getLifecycleContentById(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getLifecycleContentById(
                SecurityUtils.requireCurrentUserId(principal), id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDetailResponse>> getContentById(
            @PathVariable UUID id) {
        ContentDetailResponse response = contentService.getContentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw ContentException.validationFailed("size", "must be between 1 and 50");
        }
    }
}
