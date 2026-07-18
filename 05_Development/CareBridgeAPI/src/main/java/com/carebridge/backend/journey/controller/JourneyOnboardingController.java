package com.carebridge.backend.journey.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.journey.dto.JourneyOnboardingStatusResponse;
import com.carebridge.backend.journey.dto.SubmitJourneyOnboardingRequest;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journey-onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MOTHER')")
public class JourneyOnboardingController {

    private final IJourneyOnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<ApiResponse<JourneyOnboardingStatusResponse>> submit(
            @Valid @RequestBody SubmitJourneyOnboardingRequest request,
            Principal principal) {
        var userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.submit(userId, request), "Onboarding context saved"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<JourneyOnboardingStatusResponse>> status(
            Principal principal) {
        var userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getStatus(userId)));
    }
}
