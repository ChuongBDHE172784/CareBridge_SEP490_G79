package com.carebridge.backend.aimoderation.controller;

import com.carebridge.backend.aimoderation.dto.request.AiPolicyTestRequest;
import com.carebridge.backend.aimoderation.dto.request.AiRescanRequest;
import com.carebridge.backend.aimoderation.dto.request.CreateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.request.UpdateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.request.UpdateAiPolicyStatusRequest;
import com.carebridge.backend.aimoderation.dto.response.AiModerationStatusResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyPageResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyTestResponse;
import com.carebridge.backend.aimoderation.dto.response.AiRescanResponse;
import com.carebridge.backend.aimoderation.service.AiModerationStatusService;
import com.carebridge.backend.aimoderation.service.AiPolicyService;
import com.carebridge.backend.aimoderation.service.AiScanEnqueueService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYSTEM_ADMIN management surface for AI content moderation (policy CRUD without hard
 * delete, config/health status without secrets, sandbox test, manual rescan). Same
 * ApiResponse-envelope convention as RedFlagRuleController.
 */
@RestController
@RequestMapping("/api/v1/admin/ai-moderation")
// Authoring AI moderation policy is a SYSTEM_ADMIN responsibility. The single exception is the
// read-only /status probe, which the moderator queue uses to tell whether AI screening is live —
// see the method-level override below.
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class AiModerationAdminController {

    private final AiPolicyService policyService;
    private final AiModerationStatusService statusService;
    private final AiScanEnqueueService enqueueService;
    private final AuditService auditService;

    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<AiPolicyPageResponse>> listPolicies(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(policyService.listPolicies(active, page, Math.min(size, 100))));
    }

    @PostMapping("/policies")
    public ResponseEntity<ApiResponse<AiPolicyResponse>> createPolicy(
            @Valid @RequestBody CreateAiPolicyRequest request, Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        AiPolicyResponse response = policyService.createPolicy(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "AI moderation policy created successfully"));
    }

    @PutMapping("/policies/{id}")
    public ResponseEntity<ApiResponse<AiPolicyResponse>> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAiPolicyRequest request,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(policyService.updatePolicy(id, request, actorUserId)));
    }

    @PatchMapping("/policies/{id}/status")
    public ResponseEntity<ApiResponse<AiPolicyResponse>> updatePolicyStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAiPolicyStatusRequest request,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                ApiResponse.success(policyService.updatePolicyStatus(id, request.active(), actorUserId)));
    }

    /**
     * Read-only health probe: reports whether AI screening is enabled. The moderator pending-content
     * queue reads it to decide whether to show the "AI đang quét" badge, so MODERATOR is admitted
     * here — and only here. It exposes no policy content and mutates nothing.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AiModerationStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(statusService.status()));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<AiPolicyTestResponse>> testPolicies(
            @Valid @RequestBody AiPolicyTestRequest request, Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(policyService.testPolicies(request, actorUserId)));
    }

    @PostMapping("/rescan")
    public ResponseEntity<ApiResponse<AiRescanResponse>> rescan(
            @Valid @RequestBody AiRescanRequest request, Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        UUID jobId = enqueueService.enqueueRescan(request.targetType(), request.targetId());
        auditService.log(AuditAction.AI_RESCAN_REQUESTED, actorUserId, request.targetType().name(),
                request.targetId().toString(), "jobId=" + jobId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new AiRescanResponse(jobId, jobId != null)));
    }
}
