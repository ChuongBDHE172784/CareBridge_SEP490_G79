package com.carebridge.backend.safety.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.safety.dto.request.ImuDataRequest;
import com.carebridge.backend.safety.dto.request.SafetyEventActionRequest;
import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ISafetyConfigService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safety")
@RequiredArgsConstructor
public class FallDetectionController {

    private final IFallDetectionService fallDetectionService;
    private final ISafetyConfigService safetyConfigService;

    @PostMapping("/fall-detection/enable")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ImuMonitoringSessionResponse>> enable(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        String sensitivityLevel = safetyConfigService.getConfig(userId).getSensitivityLevel();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fallDetectionService.enable(userId, sensitivityLevel)));
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

    @GetMapping("/events")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<SafetyEventResponse>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                fallDetectionService.listSafetyEvents(userId, PageRequest.of(page, size))));
    }

    @PostMapping("/events/{eventId}/confirm")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyEventResponse>> confirmSafetyCheck(
            @PathVariable UUID eventId,
            @Valid @RequestBody SafetyEventActionRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                fallDetectionService.confirmSafetyCheck(userId, eventId, request.getNote())));
    }

    @PostMapping("/events/{eventId}/false-positive")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyEventResponse>> reportFalsePositive(
            @PathVariable UUID eventId,
            @Valid @RequestBody SafetyEventActionRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                fallDetectionService.reportFalsePositive(userId, eventId, request.getNote())));
    }

    @PostMapping("/events/{eventId}/emergency-alert")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> sendEmergencyAlert(
            @PathVariable UUID eventId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        fallDetectionService.sendEmergencyAlert(userId, eventId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(null));
    }
}
