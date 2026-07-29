package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.*;
import com.carebridge.backend.health.service.IHealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final IHealthRecordService healthRecordService;

    // UC39: Add health record
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<AddHealthRecordResponse>> addHealthRecord(
            @Valid @RequestBody AddHealthRecordRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.addHealthRecord(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Health record added successfully"));
    }

    // UC211: View health record detail
    @GetMapping("/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HealthRecordDetailResponse>> getHealthRecord(
            @PathVariable UUID recordId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.getHealthRecord(recordId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC40: Partial update health record
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<UpdateHealthRecordResponse>> updateHealthRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHealthRecordRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.updateHealthRecord(id, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Health record updated successfully"));
    }

    // UC41: Archive health record (soft-delete)
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ArchiveHealthRecordResponse>> archiveHealthRecord(
            @PathVariable UUID id,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.archiveRecord(id, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Health record archived successfully"));
    }

    // UC42: View health record timeline
    @GetMapping("/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TimelineResponse>> getTimeline(
            @Valid TimelineFilter filter,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.getTimeline(callerId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
