package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.BabyCareOverviewResponse;
import com.carebridge.backend.carejourney.service.IBabyCareOverviewService;
import com.carebridge.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class BabyCareOverviewController {
    private final IBabyCareOverviewService overviewService;

    @GetMapping("/{babyId}/care-overview")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ApiResponse<BabyCareOverviewResponse> getOverview(@PathVariable UUID babyId, Principal principal) {
        return ApiResponse.success(overviewService.getOverview(babyId, UUID.fromString(principal.getName())));
    }
}
