package com.carebridge.backend.exercise.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.exercise.dto.AdminPostureConfigResponse;
import com.carebridge.backend.exercise.dto.CreatePostureConfigRequest;
import com.carebridge.backend.exercise.dto.UpdatePostureConfigRequest;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/posture-configs")
@RequiredArgsConstructor
public class AdminPostureConfigController {

    private final IPostureConfigService postureConfigService;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AdminPostureConfigResponse>> createConfig(
            @RequestBody @Valid CreatePostureConfigRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postureConfigService.createConfig(request, adminUserId));
    }

    @PostMapping("/{exerciseId}/versions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AdminPostureConfigResponse>> createNewVersion(
            @PathVariable UUID exerciseId,
            @RequestBody @Valid UpdatePostureConfigRequest request,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postureConfigService.createNewVersion(exerciseId, request, adminUserId));
    }

    @PatchMapping("/{postureConfigId}/activate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AdminPostureConfigResponse>> activateVersion(
            @PathVariable UUID postureConfigId, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(postureConfigService.activateVersion(postureConfigId, adminUserId));
    }

    @GetMapping("/{exerciseId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminPostureConfigResponse>>> listVersions(
            @PathVariable UUID exerciseId) {
        return ResponseEntity.ok(postureConfigService.listVersions(exerciseId));
    }
}
