package com.carebridge.backend.safety.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.safety.dto.request.ImuDataRequest;
import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safety")
@RequiredArgsConstructor
public class FallDetectionController {

    private final IFallDetectionService fallDetectionService;

    @PostMapping("/fall-detection/enable")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ImuMonitoringSessionResponse>> enable(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fallDetectionService.enable(userId, "MEDIUM")));
    }

    @PostMapping("/fall-detection/disable")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> disable(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        fallDetectionService.disable(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/imu-data")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyEventResponse>> processImuData(
            @Valid @RequestBody ImuDataRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        ImuDataPayload payload = new ImuDataPayload(
                request.getAccelerometerX(), request.getAccelerometerY(), request.getAccelerometerZ(),
                request.getGyroscopeX(), request.getGyroscopeY(), request.getGyroscopeZ(),
                request.getTimestamp(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(ApiResponse.success(fallDetectionService.processImuData(userId, payload)));
    }
}
