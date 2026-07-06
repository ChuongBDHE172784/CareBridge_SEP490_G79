package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.AddMilestoneRequest;
import com.carebridge.backend.carejourney.dto.MilestoneResponse;
import com.carebridge.backend.carejourney.service.IMilestoneService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies/{babyId}/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final IMilestoneService milestoneService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<MilestoneResponse> addMilestone(
            @PathVariable UUID babyId,
            @Valid @RequestBody AddMilestoneRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        MilestoneResponse response = milestoneService.addMilestone(userId, babyId, request);
        return ApiResponse.success(response);
    }
}
