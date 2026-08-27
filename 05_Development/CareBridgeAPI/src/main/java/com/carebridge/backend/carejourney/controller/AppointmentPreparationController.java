package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.AppointmentPreparationSummaryResponse;
import com.carebridge.backend.carejourney.service.IAppointmentPreparationService;
import com.carebridge.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class AppointmentPreparationController {
    private final IAppointmentPreparationService preparationService;

    @GetMapping("/{babyId}/appointment-preparation-summary")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ApiResponse<AppointmentPreparationSummaryResponse> getSummary(@PathVariable UUID babyId, Principal principal) {
        return ApiResponse.success(preparationService.getSummary(babyId, UUID.fromString(principal.getName())));
    }
}
