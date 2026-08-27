package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.service.ITriageService;
import com.carebridge.backend.triage.service.ITriageContinuationService;
import com.carebridge.backend.triage.dto.request.ContinuationTokenRequest;
import com.carebridge.backend.triage.dto.response.ContinuationDescriptor;
import com.carebridge.backend.triage.dto.response.ContinuationAcknowledgementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/triage/intake")
@RequiredArgsConstructor
public class IntakeController {

    private final ITriageService triageService;
    private final ITriageContinuationService continuationService;

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<TriageResultResponse>> getResult(
            @PathVariable UUID sessionId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageService.getResult(sessionId, userId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<List<IntakeSessionResponse>>> listSessions(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageService.listSessions(userId)));
    }

    @PostMapping("/continuations/resolve")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ContinuationDescriptor>> resolveContinuation(
            @Valid @RequestBody ContinuationTokenRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                continuationService.resolve(userId, request.getToken())));
    }

    @PostMapping("/continuations/acknowledge")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ContinuationAcknowledgementResponse>> acknowledgeContinuation(
            @Valid @RequestBody ContinuationTokenRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        continuationService.acknowledge(userId, request.getToken());
        return ResponseEntity.ok(ApiResponse.success(
                new ContinuationAcknowledgementResponse("ACKNOWLEDGED")));
    }
}
