package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.GenerateHealthSummaryRequest;
import com.carebridge.backend.health.dto.HealthSummaryResponse;
import com.carebridge.backend.health.dto.ListHealthSummaryFilter;
import com.carebridge.backend.health.dto.ShareSummaryRequest;
import com.carebridge.backend.health.dto.ShareSummaryResponse;
import com.carebridge.backend.health.service.IHealthSummaryService;
import com.carebridge.backend.health.service.IShareSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-summaries")
@RequiredArgsConstructor
public class HealthSummaryController {

    private final IHealthSummaryService healthSummaryService;
    private final IShareSummaryService shareSummaryService;

    // UC43: Generate health summary
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<HealthSummaryResponse>> generateSummary(
            @Valid @RequestBody GenerateHealthSummaryRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthSummaryService.generateSummary(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Health summary generated successfully"));
    }

    // UC43: Get health summary by ID
    @GetMapping("/{summaryId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<HealthSummaryResponse>> getSummary(
            @PathVariable UUID summaryId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthSummaryService.getSummary(summaryId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC43: List health summaries
    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<HealthSummaryResponse>>> listSummaries(
            @Valid ListHealthSummaryFilter filter,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = healthSummaryService.listSummaries(callerId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC44: Share summary with expert
    @PostMapping("/share")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ShareSummaryResponse>> shareSummary(
            @Valid @RequestBody ShareSummaryRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = shareSummaryService.shareSummary(request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Summary shared with expert successfully"));
    }
}
