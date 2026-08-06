package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.TriageV2ContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageV2StartRequest;
import com.carebridge.backend.triage.dto.response.TriageV2SessionResponse;
import com.carebridge.backend.triage.service.ITriageV2SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/** Feature-flagged V2 boundary. The default configuration responds as not found. */
@RestController
@RequestMapping("/api/internal/v2/triage/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
public class InternalTriageV2Controller {
    private final ITriageV2SessionService service;

    @PostMapping
    public ResponseEntity<ApiResponse<TriageV2SessionResponse>> start(
            @Valid @RequestBody TriageV2StartRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.start(request, userId)));
    }

    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<TriageV2SessionResponse>> continueSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TriageV2ContinueRequest request,
            Principal principal) {
        if (!sessionId.equals(request.sessionId())) {
            throw new IllegalArgumentException("Session path and body must match");
        }
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.continueSession(request, userId)));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<TriageV2SessionResponse>> get(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.get(sessionId, userId)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<TriageV2SessionResponse>> cancel(
            @PathVariable UUID sessionId,
            @RequestParam int expectedStateVersion,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                service.cancel(sessionId, expectedStateVersion, userId)));
    }
}
