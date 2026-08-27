package com.carebridge.backend.systemconfiguration.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.systemconfiguration.dto.request.UpdateSystemConfigurationRequest;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import com.carebridge.backend.systemconfiguration.service.SystemConfigurationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system-configuration")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class SystemConfigurationController {
    private final SystemConfigurationService service;

    @GetMapping
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> get(Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.get(actorId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> update(
            @Valid @RequestBody UpdateSystemConfigurationRequest request, Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.update(request, actorId), "System configuration updated"));
    }
}
