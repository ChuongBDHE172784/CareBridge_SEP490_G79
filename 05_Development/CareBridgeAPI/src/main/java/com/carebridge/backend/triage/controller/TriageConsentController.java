package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.AcceptTriageConsentRequest;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.dto.response.TriageConsentStatusResponse;
import com.carebridge.backend.triage.service.ITriageConsentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §9 — AI-triage disclaimer consent endpoints (ROLE_MOTHER only,
 * TDS §16). Validation + request/response mapping only; all business logic lives in
 * {@link ITriageConsentService} (BR-TDC-006 / CLAUDE.md architecture rules).
 */
@RestController
@RequestMapping("/api/v1/triage/consent")
@RequiredArgsConstructor
public class TriageConsentController {

    private final ITriageConsentService triageConsentService;

    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<TriageConsentStatusResponse>> getStatus(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageConsentService.getStatus(userId)));
    }

    @PostMapping("/accept")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<TriageConsentStatusResponse>> accept(
            @Valid @RequestBody AcceptTriageConsentRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        TriageConsentAcceptOutcome outcome = triageConsentService.accept(request, userId);
        // created → 201 (new consent row); idempotent re-accept → 200 (TDS §9.1)
        HttpStatus httpStatus = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success(outcome.status()));
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<TriageConsentStatusResponse>> revoke(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageConsentService.revoke(userId)));
    }
}
