package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.service.IHealthMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-metrics")
@RequiredArgsConstructor
public class HealthMetricController {

    private final IHealthMetricService healthMetricService;

    // UC187: View maternal health metric detail
    @GetMapping("/{metricId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<MetricDetailResponse>> getMetricDetail(
            @PathVariable UUID metricId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthMetricService.getMetricDetail(metricId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
