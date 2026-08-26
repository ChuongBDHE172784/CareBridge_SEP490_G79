package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// UC-243 §14 addendum — coexists on the same base path as AdminChecklistTemplateController,
// exactly like ContentApprovalController coexists with AdminContentController on /api/v1/admin/content.
@RestController
@RequestMapping("/api/v1/admin/checklist-templates")
@RequiredArgsConstructor
public class ChecklistTemplateApprovalController {

    private final ChecklistTemplateApprovalService checklistTemplateApprovalService;
    private final com.carebridge.backend.content.service.ExpertContentApprovalService expertContentApprovalService;

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EXPERT')")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> decide(
            @PathVariable UUID id,
            @Valid @RequestBody ContentDecisionRequest request,
            Principal principal) {
        ChecklistTemplateDecisionResponse response = checklistTemplateApprovalService.decide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist template decision recorded successfully"));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reassign(
            @PathVariable UUID id,
            @Valid @RequestBody com.carebridge.backend.content.dto.request.ReassignContentRequest request,
            Principal principal) {
        expertContentApprovalService.reassignChecklist(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Checklist template reassigned successfully"));
    }

    @PostMapping("/{lineageId}/versions/{versionId}/approve")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> approveVersion(
            @PathVariable UUID lineageId, @PathVariable UUID versionId, Principal principal) {
        ChecklistTemplateDecisionResponse response = checklistTemplateApprovalService.decideInLineage(
                lineageId, versionId, new ContentDecisionRequest(
                        com.carebridge.backend.content.entity.ContentDecision.APPROVE, null), principal);
        if (response.id() == null) {
            return ResponseEntity.ok(ApiResponse.success(response, "Checklist template decision recorded successfully"));
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist template version approved"));
    }

    @PostMapping("/{lineageId}/versions/{versionId}/review")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> reviewImportedVersion(
            @PathVariable UUID lineageId, @PathVariable UUID versionId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                checklistTemplateApprovalService.reviewImportedInLineage(lineageId, versionId, principal),
                "Migrated checklist template reviewed"));
    }

    @PostMapping("/{lineageId}/versions/{versionId}/activate")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> activateImportedVersion(
            @PathVariable UUID lineageId, @PathVariable UUID versionId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                checklistTemplateApprovalService.activateImportedInLineage(lineageId, versionId, principal),
                "Migrated checklist template activated"));
    }
}
