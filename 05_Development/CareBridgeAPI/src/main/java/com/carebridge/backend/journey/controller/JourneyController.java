package com.carebridge.backend.journey.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.service.IJourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final IJourneyService journeyService;

    // UC22: Create mother journey
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateJourneyResponse>> createJourney(
            @Valid @RequestBody CreateJourneyRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = journeyService.createJourney(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Journey created successfully"));
    }

    // UC23: Update mother journey
    @PutMapping("/{journeyId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyResponse>> updateJourney(
            @PathVariable UUID journeyId,
            @Valid @RequestBody UpdateJourneyRequest request,
            Principal principal) {
        var ownerId = SecurityUtils.requireCurrentUserId(principal);
        var response = journeyService.updateJourney(ownerId, journeyId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Journey updated successfully"));
    }

    // UC24: View mother journey dashboard — /me pattern prevents IDOR (ADR-JOURNEY-003-003)
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyDashboardResponse>> getDashboard(Principal principal) {
        var userId = SecurityUtils.requireCurrentUserId(principal);
        var dashboard = journeyService.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
