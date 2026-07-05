package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.service.IGrowthService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class GrowthChartController {

    private final IGrowthService growthService;

    @GetMapping("/{babyId}/growth-chart")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<GrowthChartResponse>> getGrowthChart(
            @PathVariable UUID babyId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        GrowthChartResponse response = growthService.getGrowthChart(userId, babyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
