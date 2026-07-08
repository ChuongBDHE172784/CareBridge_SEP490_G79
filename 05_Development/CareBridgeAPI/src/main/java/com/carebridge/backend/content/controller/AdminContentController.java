package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.request.HideContentRequest;
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
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

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateContentResponse>> createContent(
            @Valid @RequestBody CreateContentRequest request,
            Principal principal) {
        java.util.UUID authorUserId = SecurityUtils.requireCurrentUserId(principal);
        CreateContentResponse response = adminContentService.createContent(request, authorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Content created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UpdateContentResponse>> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest request,
            Principal principal) {
        UpdateContentResponse response = adminContentService.updateContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Content updated successfully"));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<HideContentResponse>> hideContent(
            @PathVariable UUID id,
            @Valid @RequestBody HideContentRequest request,
            Principal principal) {
        HideContentResponse response = adminContentService.hideContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Content archived successfully"));
    }
}
