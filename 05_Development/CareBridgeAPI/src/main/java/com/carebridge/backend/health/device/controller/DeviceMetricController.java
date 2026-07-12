package com.carebridge.backend.health.device.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.device.dto.DeviceTrendQuery;
import com.carebridge.backend.health.device.dto.DeviceTrendResponse;
import com.carebridge.backend.health.device.dto.ImportDeviceMetricRequest;
import com.carebridge.backend.health.device.dto.ImportDeviceMetricResponse;
import com.carebridge.backend.health.device.service.IDeviceDataImportService;
import com.carebridge.backend.health.device.service.IDeviceTrendService;
import com.carebridge.backend.health.entity.MetricType;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health/metrics")
@RequiredArgsConstructor
public class DeviceMetricController {

    private final IDeviceDataImportService importService;
    private final IDeviceTrendService trendService;

    @PostMapping("/device-import")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ImportDeviceMetricResponse>> importMetric(
            @Valid @RequestBody ImportDeviceMetricRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(importService.importMetric(request, userId)));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<DeviceTrendResponse>> trend(
            @RequestParam UUID journeyId,
            @RequestParam MetricType metricType,
            @RequestParam Instant from,
            @RequestParam Instant to,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(trendService.getTrend(
                new DeviceTrendQuery(journeyId, metricType, from, to), userId)));
    }
}
