package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.service.IHealthMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys/{journeyId}/metrics")
@RequiredArgsConstructor
public class JourneyMetricController {

    private final IHealthMetricService healthMetricService;

    // UC25: Add maternal health metric
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<MetricResponse>> addMetric(
            @PathVariable UUID journeyId,
            @Valid @RequestBody AddMetricRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthMetricService.addMetric(callerId, journeyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // UC26: Update maternal health metric
    @PutMapping("/{metricId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<MetricResponse>> updateMetric(
            @PathVariable UUID journeyId,
            @PathVariable UUID metricId,
            @Valid @RequestBody UpdateMetricRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthMetricService.updateMetric(callerId, journeyId, metricId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC27: View maternal health metric trend
    @GetMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'MEMBER')")
    public ResponseEntity<ApiResponse<MetricTrendResponse>> getMetricTrend(
            @PathVariable UUID journeyId,
            @RequestParam MetricType metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        Instant resolvedFrom = from != null ? from : Instant.now().minus(90, ChronoUnit.DAYS);
        Instant resolvedTo = to != null ? to : Instant.now();
        var response = healthMetricService.getMetricTrend(callerId, journeyId, metricType, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
