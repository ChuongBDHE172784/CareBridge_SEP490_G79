package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.CreateEvidenceSourceRequest;
import com.carebridge.backend.triage.dto.request.ReviewEvidenceSourceRequest;
import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.entity.EvidenceSourceReviewLog;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/api/v1/evidence-sources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CONTENT_ADMIN')")
public class EvidenceSourceAdminController {
    private final EvidenceSourceService evidenceSourceService;

    @PostMapping
    public ApiResponse<EvidenceSource> create(@Valid @RequestBody CreateEvidenceSourceRequest request, Principal principal) {
        UUID actor = SecurityUtils.requireCurrentUserId(principal);
        return ApiResponse.success(evidenceSourceService.propose(
                request.getBaseUrl(),
                request.getOrganization(),
                request.getCategory(),
                request.getApplicableStages(),
                request.getNotes(),
                actor));
    }

    @GetMapping
    public ApiResponse<List<EvidenceSource>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(evidenceSourceService.list(status));
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<EvidenceSource> approve(@PathVariable UUID id, @Valid @RequestBody(required = false) ReviewEvidenceSourceRequest request, Principal principal) {
        return ApiResponse.success(evidenceSourceService.changeStatus(id, "APPROVED",
                request == null ? null : request.getNotes(), SecurityUtils.requireCurrentUserId(principal), "REVIEWER"));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<EvidenceSource> reject(@PathVariable UUID id, @Valid @RequestBody(required = false) ReviewEvidenceSourceRequest request, Principal principal) {
        return ApiResponse.success(evidenceSourceService.changeStatus(id, "ARCHIVED",
                request == null ? null : request.getNotes(), SecurityUtils.requireCurrentUserId(principal), "REVIEWER"));
    }

    @PatchMapping("/{id}/deprecate")
    public ApiResponse<EvidenceSource> deprecate(@PathVariable UUID id, @Valid @RequestBody(required = false) ReviewEvidenceSourceRequest request, Principal principal) {
        return ApiResponse.success(evidenceSourceService.changeStatus(id, "DEPRECATED",
                request == null ? null : request.getNotes(), SecurityUtils.requireCurrentUserId(principal), "REVIEWER"));
    }

    @GetMapping("/{id}/review-log")
    public ApiResponse<List<EvidenceSourceReviewLog>> reviewLog(@PathVariable UUID id) {
        return ApiResponse.success(evidenceSourceService.reviewLog(id));
    }
}
