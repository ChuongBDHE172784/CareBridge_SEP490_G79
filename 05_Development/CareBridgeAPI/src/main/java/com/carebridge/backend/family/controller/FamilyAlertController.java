package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.FamilyAlertListResponse;
import com.carebridge.backend.family.service.IFamilyAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/family-alerts")
@RequiredArgsConstructor
public class FamilyAlertController {

    private final IFamilyAlertService familyAlertService;

    // UC86: List family alerts (EMERGENCY notifications) for the authenticated user
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FamilyAlertListResponse>> listFamilyAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = familyAlertService.listFamilyAlerts(callerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
