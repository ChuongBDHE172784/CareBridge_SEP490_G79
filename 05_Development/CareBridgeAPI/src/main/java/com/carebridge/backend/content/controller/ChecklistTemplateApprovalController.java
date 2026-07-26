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
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class ChecklistTemplateApprovalController {

    private final ChecklistTemplateApprovalService checklistTemplateApprovalService;

    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> decide(
            @PathVariable UUID id,
            @Valid @RequestBody ContentDecisionRequest request,
            Principal principal) {
        ChecklistTemplateDecisionResponse response = checklistTemplateApprovalService.decide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist template decision recorded successfully"));
    }
}
