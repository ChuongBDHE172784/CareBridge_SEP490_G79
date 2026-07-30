package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.FamilyDashboardResponse;
import com.carebridge.backend.family.service.FamilyDashboardService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
public class FamilyDashboardController {

    private final FamilyDashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FamilyDashboardResponse>> getDashboard(
            @RequestParam(required = false) UUID selectedCareGroupId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.get(userId, selectedCareGroupId)));
    }
}
