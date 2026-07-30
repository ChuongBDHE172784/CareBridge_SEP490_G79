package com.carebridge.backend.map.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import com.carebridge.backend.map.service.ICareFacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/facilities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminCareFacilityController {

    private final ICareFacilityService careFacilityService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getPendingFacilities() {
        return ResponseEntity.ok(ApiResponse.success(careFacilityService.getPendingFacilities()));
    }

    @PostMapping("/{facilityId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyFacility(
            @PathVariable UUID facilityId,
            @RequestParam FacilityStatus status,
            Principal principal) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        careFacilityService.verifyFacility(facilityId, status, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Facility status updated to " + status));
    }
}
