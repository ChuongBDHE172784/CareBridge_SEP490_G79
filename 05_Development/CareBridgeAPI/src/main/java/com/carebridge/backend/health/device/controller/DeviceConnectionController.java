package com.carebridge.backend.health.device.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.device.dto.ConnectDeviceRequest;
import com.carebridge.backend.health.device.dto.DeviceConnectionResponse;
import com.carebridge.backend.health.device.service.IDeviceConnectionService;
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
@RequestMapping("/api/v1/health/devices/connections")
@RequiredArgsConstructor
public class DeviceConnectionController {

    private final IDeviceConnectionService deviceConnectionService;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<DeviceConnectionResponse>> connect(
            @Valid @RequestBody ConnectDeviceRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deviceConnectionService.connect(request, userId)));
    }

    @PatchMapping("/{id}/disconnect")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<DeviceConnectionResponse>> disconnect(
            @PathVariable UUID id,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(deviceConnectionService.disconnect(id, userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<DeviceConnectionResponse>>> list(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(deviceConnectionService.listConnections(userId)));
    }
}
