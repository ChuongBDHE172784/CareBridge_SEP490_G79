package com.carebridge.backend.emergency.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.service.IEmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency/sessions")
@RequiredArgsConstructor
public class EmergencyController {

    private final IEmergencyService emergencyService;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencySessionResponse>> openFlow(
            @Valid @RequestBody OpenEmergencyRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        EmergencySessionResponse response = emergencyService.openFlow(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencySessionResponse>> getActive(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(emergencyService.getActiveSession(userId)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencySessionResponse>> resolve(
            @PathVariable UUID id,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(emergencyService.resolveSession(id, userId)));
    }
}
