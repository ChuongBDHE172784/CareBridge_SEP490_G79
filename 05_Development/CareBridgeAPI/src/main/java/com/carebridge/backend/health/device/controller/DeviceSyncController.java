package com.carebridge.backend.health.device.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.device.dto.DeviceSyncResultResponse;
import com.carebridge.backend.health.device.service.IDeviceSyncService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health/devices/connections")
@RequiredArgsConstructor
public class DeviceSyncController {

    private final IDeviceSyncService syncService;

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<DeviceSyncResultResponse>> syncNow(
            @PathVariable UUID id,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(syncService.syncNow(id, userId)));
    }
}
