package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.AdminChecklistTemplateService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import com.carebridge.backend.content.dto.response.ChecklistTemplateVersionSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// UC-243 (CB-CONTENT-IMP-011) — ADR-CHK-001: read open to CONTENT_ADMIN+SYSTEM_ADMIN (§14 addendum,
// SYSTEM_ADMIN needs list/detail for the approval queue), writes overridden to CONTENT_ADMIN-only.
@RestController
@RequestMapping("/api/v1/admin/checklist-templates")
@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class AdminChecklistTemplateController {

    private final AdminChecklistTemplateService adminChecklistTemplateService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminChecklistTemplateDetailResponse>>> list(
            @RequestParam(required = false) ChecklistTemplateStatus status,
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw ContentException.validationFailed("size", "must be between 1 and 50");
        }
        return ResponseEntity.ok(ApiResponse.success(
                adminChecklistTemplateService.list(status, stage, PageRequest.of(page, size)),
                "Checklist template workspace loaded"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminChecklistTemplateService.getById(id), "Checklist template loaded"));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<ChecklistTemplateVersionSnapshotResponse>>> getVersionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminChecklistTemplateService.getVersionHistory(id), "Checklist version history loaded"));
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>> create(
            @Valid @RequestBody CreateChecklistTemplateRequest request,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        AdminChecklistTemplateDetailResponse response =
                adminChecklistTemplateService.create(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Checklist template created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChecklistTemplateRequest request,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        AdminChecklistTemplateDetailResponse response =
                adminChecklistTemplateService.update(id, request, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist template updated successfully"));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>> cloneVersion(
            @PathVariable UUID id, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                adminChecklistTemplateService.cloneVersion(id, adminUserId),
                "Checklist template draft cloned successfully"));
    }

    @PostMapping("/{lineageId}/versions/{versionId}/clone")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>> cloneVersionInLineage(
            @PathVariable UUID lineageId, @PathVariable UUID versionId, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        AdminChecklistTemplateDetailResponse response = adminChecklistTemplateService
                .cloneVersionInLineage(lineageId, versionId, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                response, "Checklist template draft cloned successfully"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<HideChecklistTemplateResponse>> archive(
            @PathVariable UUID id,
            @Valid @RequestBody HideChecklistTemplateRequest request,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        HideChecklistTemplateResponse response = adminChecklistTemplateService.archive(id, request, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist template archived successfully"));
    }
}
