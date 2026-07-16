package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.BabyCareTimelineResponse;
import com.carebridge.backend.carejourney.service.IBabyCareTimelineService;
import com.carebridge.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class BabyCareTimelineController {
    private final IBabyCareTimelineService timelineService;

    @GetMapping("/{babyId}/care-timeline")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ApiResponse<BabyCareTimelineResponse> getTimeline(@PathVariable UUID babyId,
                                                               @RequestParam(required = false) String cursor,
                                                               @RequestParam(defaultValue = "50") int size,
                                                               Principal principal) {
        return ApiResponse.success(timelineService.getTimeline(babyId, cursor, size, UUID.fromString(principal.getName())));
    }
}
