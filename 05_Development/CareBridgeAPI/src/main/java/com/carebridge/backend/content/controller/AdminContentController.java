package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.request.HideContentRequest;
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.AdminContentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateResponse;
import com.carebridge.backend.common.response.PaginatedResponse;

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;
    private final ContentService contentService;

    @GetMapping("/checklists")
    public ResponseEntity<PaginatedResponse<AdminChecklistTemplateResponse>> getChecklists(
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(required = false) ChecklistTemplateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw ContentException.validationFailed("size", "must be between 1 and 50");
        }
        return ResponseEntity.ok(PaginatedResponse.of(
                contentService.getAdminChecklists(stage, status, PageRequest.of(page, size))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StaffContentDetailResponse>>> getContents(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw ContentException.validationFailed("size", "must be between 1 and 50");
        }
        return ResponseEntity.ok(ApiResponse.success(
                adminContentService.getStaffContents(status, type, stage, keyword, PageRequest.of(page, size)),
                "Content workspace loaded"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffContentDetailResponse>> getContent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminContentService.getStaffContent(id), "Content loaded"));
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<CreateContentResponse>> createContent(
            @Valid @RequestBody CreateContentRequest request,
            Principal principal) {
        java.util.UUID authorUserId = SecurityUtils.requireCurrentUserId(principal);
        CreateContentResponse response = adminContentService.createContent(request, authorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Content created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateContentResponse>> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest request,
            Principal principal) {
        UpdateContentResponse response = adminContentService.updateContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Content updated successfully"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<HideContentResponse>> hideContent(
            @PathVariable UUID id,
            @Valid @RequestBody HideContentRequest request,
            Principal principal) {
        HideContentResponse response = adminContentService.hideContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Content archived successfully"));
    }
}
