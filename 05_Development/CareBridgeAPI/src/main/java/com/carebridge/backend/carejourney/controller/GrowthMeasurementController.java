package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.AddGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementHistoryItem;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementResponse;
import com.carebridge.backend.carejourney.dto.UpdateGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.service.IGrowthService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class GrowthMeasurementController {

    private final IGrowthService growthService;

    @PostMapping("/{babyId}/growth-measurements")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<GrowthMeasurementResponse>> addGrowthMeasurement(
            @PathVariable UUID babyId,
            @Valid @RequestBody AddGrowthMeasurementRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        GrowthMeasurementResponse response = growthService.addGrowthMeasurement(userId, babyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{babyId}/growth-measurements/{growthMeasurementId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<GrowthMeasurementResponse>> updateGrowthMeasurement(
            @PathVariable UUID babyId,
            @PathVariable UUID growthMeasurementId,
            @Valid @RequestBody UpdateGrowthMeasurementRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        GrowthMeasurementResponse response =
                growthService.updateGrowthMeasurement(userId, babyId, growthMeasurementId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{babyId}/growth-measurements/{growthMeasurementId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<Void> deleteGrowthMeasurement(
            @PathVariable UUID babyId,
            @PathVariable UUID growthMeasurementId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        growthService.deleteGrowthMeasurement(userId, babyId, growthMeasurementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{babyId}/growth-measurements")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<Page<GrowthMeasurementHistoryItem>>> getGrowthMeasurementHistory(
            @PathVariable UUID babyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-084",
                    "Invalid growth measurement history pagination");
        }
        Page<GrowthMeasurementHistoryItem> response =
                growthService.getGrowthMeasurementHistory(userId, babyId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
