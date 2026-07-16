package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.BabyLogSummaryResponse;
import com.carebridge.backend.carejourney.service.IBabyLogSummaryService;
import com.carebridge.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies/{babyId}/daily-logs")
@RequiredArgsConstructor
public class BabyLogSummaryController {

    private final IBabyLogSummaryService babyLogSummaryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ApiResponse<BabyLogSummaryResponse> getSummary(
            @PathVariable UUID babyId,
            @RequestParam(defaultValue = "24h") String period,
            Principal principal) {
        BabyLogSummaryResponse response = babyLogSummaryService.getSummary(babyId, period, principal);
        return ApiResponse.success(response);
    }
}
