package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.service.ContentApprovalService;
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

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class ContentApprovalController {

    private final ContentApprovalService contentApprovalService;

    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<ContentDecisionResponse>> decide(
            @PathVariable UUID id,
            @Valid @RequestBody ContentDecisionRequest request,
            Principal principal) {
        ContentDecisionResponse response = contentApprovalService.decide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Content decision recorded successfully"));
    }
}
