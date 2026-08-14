package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.TriageSessionContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageSessionStartRequest;
import com.carebridge.backend.triage.dto.response.TriageSessionResponse;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.service.ITriageSessionService;
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

/** Canonical session boundary. The versioned internal path is a transition-only route alias. */
@RestController
@RequestMapping("/api/v1/triage/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
public class TriageSessionController {
    private final ITriageSessionService service;

    @PostMapping
    public ResponseEntity<ApiResponse<TriageSessionResponse>> start(
            @Valid @RequestBody TriageSessionStartRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.start(request, userId)));
    }

    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<TriageSessionResponse>> continueSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TriageSessionContinueRequest request,
            Principal principal) {
        if (!sessionId.equals(request.sessionId())) {
            throw new TriageException(HttpStatus.BAD_REQUEST,
                    "TRIAGE_SESSION_ID_MISMATCH", "Session path and body must match");
        }
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.continueSession(request, userId)));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<TriageSessionResponse>> get(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.get(sessionId, userId)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<TriageSessionResponse>> cancel(
            @PathVariable UUID sessionId,
            @RequestParam int expectedStateVersion,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                service.cancel(sessionId, expectedStateVersion, userId)));
    }
}
