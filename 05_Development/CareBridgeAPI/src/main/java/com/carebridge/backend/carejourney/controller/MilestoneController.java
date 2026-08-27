package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.AddMilestoneRequest;
import com.carebridge.backend.carejourney.dto.MilestoneResponse;
import com.carebridge.backend.carejourney.dto.UpdateDevelopmentMilestoneRequest;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/babies/{babyId}/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final IMilestoneService milestoneService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MilestoneResponse>> listMilestones(
            @PathVariable UUID babyId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ApiResponse.success(milestoneService.listMilestones(babyId, userId));
    }

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

    @PatchMapping("/{milestoneId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<MilestoneResponse> updateMilestone(
            @PathVariable UUID babyId,
            @PathVariable UUID milestoneId,
            @RequestBody UpdateDevelopmentMilestoneRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        MilestoneResponse response = milestoneService.updateMilestone(babyId, milestoneId, request, userId);
        return ApiResponse.success(response, "Development milestone updated successfully");
    }

    @DeleteMapping("/{milestoneId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ApiResponse<Void> deleteMilestone(
            @PathVariable UUID babyId,
            @PathVariable UUID milestoneId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        milestoneService.deleteMilestone(babyId, milestoneId, userId);
        return ApiResponse.success(null, "Development milestone deleted successfully");
    }
}
