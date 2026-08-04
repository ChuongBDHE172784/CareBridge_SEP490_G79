package com.carebridge.backend.safety.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.safety.dto.request.SensorSelfTestCompletionRequest;
import com.carebridge.backend.safety.dto.request.SensorSelfTestEventRequest;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.service.impl.SensorSelfTestService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/safety/events")
@RequiredArgsConstructor
public class SensorSelfTestController {

    private final SensorSelfTestService sensorSelfTestService;

    @PostMapping("/sensor-self-test")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyEventResponse>> create(
            @Valid @RequestBody SensorSelfTestEventRequest request, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sensorSelfTestService.create(userId, request)));
    }

    @PostMapping("/{eventId}/sensor-self-test/complete")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<SafetyEventResponse>> complete(
            @PathVariable UUID eventId,
            @Valid @RequestBody SensorSelfTestCompletionRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(sensorSelfTestService.complete(userId, eventId, request)));
    }
}
