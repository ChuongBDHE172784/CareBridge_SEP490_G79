package com.carebridge.backend.safety.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.safety.dto.request.SafetyConfigRequest;
import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import com.carebridge.backend.safety.service.ISafetyConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safety/config")
@RequiredArgsConstructor
public class SafetyConfigController {

    private final ISafetyConfigService safetyConfigService;

    @PutMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyConfigResponse>> configure(
            @Valid @RequestBody SafetyConfigRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(safetyConfigService.configure(request, userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyConfigResponse>> getConfig(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(safetyConfigService.getConfig(userId)));
    }
}
