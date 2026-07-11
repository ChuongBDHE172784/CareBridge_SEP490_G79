package com.carebridge.backend.expert.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expert.dto.response.ContributionPointResponse;
import com.carebridge.backend.expert.service.IContributionPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert/contribution-points")
@RequiredArgsConstructor
public class ContributionPointController {

    private final IContributionPointService contributionPointService;

    private UUID userId(Principal principal) {
        return SecurityUtils.requireCurrentUserId(principal);
    }

    // UC-69: Get total points for the current user
    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('EXPERT','SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getTotal(Principal principal) {
        int total = contributionPointService.getTotalPoints(userId(principal));
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    // UC-69: Get points breakdown by source type
    @GetMapping("/breakdown")
    @PreAuthorize("hasAnyRole('EXPERT','SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getBreakdown(Principal principal) {
        int total = contributionPointService.getTotalPoints(userId(principal));
        return ResponseEntity.ok(ApiResponse.success(Map.of("TOTAL", total)));
    }

    // UC-69: Get recent contribution point records
    @GetMapping
    @PreAuthorize("hasAnyRole('EXPERT','SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ContributionPointResponse>>> getRecent(
            Principal principal,
            @RequestParam(defaultValue = "20") int size) {
        UUID uid = userId(principal);
        int lim = Math.min(size, 100);
        List<ContributionPointResponse> records = contributionPointService.getRecentPoints(uid, lim);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
