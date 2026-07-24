package com.carebridge.backend.emergency.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import com.carebridge.backend.emergency.service.IEmergencyMapHandoffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map/emergency")
@RequiredArgsConstructor
public class EmergencyMapHandoffController {

    private final IEmergencyMapHandoffService emergencyMapHandoffService;

    @PostMapping("/handoff")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencyHandoffResponse>> createHandoff(
            Principal principal,
            @Valid @RequestBody CreateEmergencyHandoffRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(emergencyMapHandoffService.createHandoff(userId, request)));
    }

    @GetMapping("/{handoffId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyHandoffResponse>> getHandoff(
            Principal principal,
            @PathVariable UUID handoffId) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        boolean systemAdmin = SecurityUtils.hasRole("SYSTEM_ADMIN");
        return ResponseEntity.ok(ApiResponse.success(
                emergencyMapHandoffService.getHandoff(handoffId, callerId, systemAdmin)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<EmergencyHandoffResponse>>> getMyHandoffs(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(emergencyMapHandoffService.getMyHandoffs(userId)));
    }
}
