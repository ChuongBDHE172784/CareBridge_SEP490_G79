package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.dto.response.ExpertContentApprovalQueueItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.ExpertContentApprovalService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/content-approval")
@PreAuthorize("hasRole('EXPERT')")
@RequiredArgsConstructor
public class ExpertContentApprovalController {

    private final ExpertContentApprovalService expertContentApprovalService;

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Page<ExpertContentApprovalQueueItem>>> getQueue(
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) ContentStage stage,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal) {
        Page<ExpertContentApprovalQueueItem> page = expertContentApprovalService.getAssignedQueue(
                type, stage, keyword, pageable, principal);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy hàng đợi thẩm định thành công"));
    }

    @PostMapping("/content/{id}/decision")
    public ResponseEntity<ApiResponse<ContentDecisionResponse>> decideContent(
            @PathVariable UUID id,
            @Valid @RequestBody ContentDecisionRequest request,
            Principal principal) {
        ContentDecisionResponse response = expertContentApprovalService.decideContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã ghi nhận quyết định thẩm định nội dung"));
    }

    @PostMapping("/checklists/{id}/decision")
    public ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>> decideChecklist(
            @PathVariable UUID id,
            @Valid @RequestBody ContentDecisionRequest request,
            Principal principal) {
        ChecklistTemplateDecisionResponse response = expertContentApprovalService.decideChecklist(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã ghi nhận quyết định thẩm định checklist"));
    }
}
