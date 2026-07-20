package com.carebridge.backend.journey.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.service.IJourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
@Validated
public class JourneyController {

    private final IJourneyService journeyService;

    // UC22: Create mother journey
    @PostMapping
    @PreAuthorize("isAuthenticated()")
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

    @GetMapping("/{journeyId}/history")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyTransitionPageResponse>> getHistory(
            @PathVariable UUID journeyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        var ownerId = SecurityUtils.requireCurrentUserId(principal);
        var history = journeyService.getHistory(
                ownerId, journeyId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // UC24: View mother journey dashboard — /me pattern prevents IDOR (ADR-JOURNEY-003-003)
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'MEMBER')")
    public ResponseEntity<ApiResponse<JourneyDashboardResponse>> getDashboard(Principal principal) {
        var userId = SecurityUtils.requireCurrentUserId(principal);
        var dashboard = journeyService.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
