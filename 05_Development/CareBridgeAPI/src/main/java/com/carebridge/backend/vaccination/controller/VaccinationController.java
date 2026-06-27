package com.carebridge.backend.vaccination.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.vaccination.dto.VaccinationScheduleResponse;
import com.carebridge.backend.vaccination.service.IVaccinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vaccination")
@RequiredArgsConstructor
public class VaccinationController {

    private final IVaccinationService vaccinationService;

    // UC228: View vaccination schedule for baby
    @GetMapping("/babies/{babyId}/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VaccinationScheduleResponse>> getVaccinationSchedule(
            @PathVariable UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.getVaccinationSchedule(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
