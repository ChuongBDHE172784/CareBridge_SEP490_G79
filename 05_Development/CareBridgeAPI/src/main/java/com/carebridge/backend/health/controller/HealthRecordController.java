package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;
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
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<HealthRecordDetailResponse>> getHealthRecord(
            @PathVariable UUID recordId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthRecordService.getHealthRecord(recordId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
