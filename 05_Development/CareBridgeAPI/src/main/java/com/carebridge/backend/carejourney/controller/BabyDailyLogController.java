package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.AddBabyDailyLogRequest;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.BabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.UpdateBabyDailyLogRequest;
import com.carebridge.backend.carejourney.service.IBabyDailyLogService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies/{babyId}/daily-logs")
@RequiredArgsConstructor
public class BabyDailyLogController {

    private final IBabyDailyLogService babyDailyLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<AddBabyDailyLogResponse> addDailyLog(
            @PathVariable UUID babyId,
            @Valid @RequestBody AddBabyDailyLogRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        AddBabyDailyLogResponse response = babyDailyLogService.addDailyLog(babyId, request, userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{logId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<BabyDailyLogResponse> updateLog(
            @PathVariable UUID babyId,
            @PathVariable UUID logId,
            @Valid @RequestBody UpdateBabyDailyLogRequest request,
            Principal principal) {
        BabyDailyLogResponse response = babyDailyLogService.updateLog(babyId, logId, request, principal);
        return ApiResponse.success(response, "Baby daily log updated successfully");
    }

    @GetMapping("/{logId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BabyDailyLogResponse> getDailyLogDetail(
            @PathVariable UUID babyId,
            @PathVariable UUID logId,
            Principal principal) {
        BabyDailyLogResponse response = babyDailyLogService.getDailyLogDetail(babyId, logId, principal);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{logId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<Void> deleteLog(
            @PathVariable UUID babyId,
            @PathVariable UUID logId,
            Principal principal) {
        babyDailyLogService.deleteLog(babyId, logId, principal);
        return ApiResponse.success(null, "Baby daily log deleted successfully");
    }
}
