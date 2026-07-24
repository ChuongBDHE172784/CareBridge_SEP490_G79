package com.carebridge.backend.consultation.context.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.HandoffPreviewResponse;
import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.service.ITriageExpertHandoffService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TriageExpertHandoffController {

    private final ITriageExpertHandoffService service;

    public TriageExpertHandoffController(ITriageExpertHandoffService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/triage/intake/{intakeSessionId}/expert-handoff-preview")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<HandoffPreviewResponse>> preview(
            @PathVariable UUID intakeSessionId, Principal principal) {
        UUID ownerUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.preview(intakeSessionId, ownerUserId)));
    }

    @PostMapping("/api/v1/triage/intake/{intakeSessionId}/expert-handoffs")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<HandoffCreateResponse>> create(
            @PathVariable UUID intakeSessionId,
            @RequestBody TriageExpertHandoffCreateRequest request,
            Principal principal) {
        UUID ownerUserId = SecurityUtils.requireCurrentUserId(principal);
        HandoffCreateResponse response = service.create(intakeSessionId, request, ownerUserId);
        HttpStatus status = response.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/consultation-requests/{requestId}/triage-context")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<HandoffParticipantResponse>> read(
            @PathVariable UUID requestId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.read(requestId, currentUserId)));
    }
}
