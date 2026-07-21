package com.carebridge.backend.baby.controller;

import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import com.carebridge.backend.baby.security.BabyLinkBoundaryAuditFilter;

@RestController @Validated @RequiredArgsConstructor
@RequestMapping("/api/v1/journeys")
public class JourneyBabiesController {
    private final IBabyService babyService;
    @GetMapping("/{journeyId}/babies") @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<PaginatedResponse<BabyProfileDetailResponse>> list(
            @PathVariable UUID journeyId,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            Principal principal,
            HttpServletRequest httpRequest) {
        BabyLinkBoundaryAuditFilter.markControllerEntered(httpRequest);
        return ResponseEntity.ok(babyService.listJourneyBabies(journeyId,page,size,SecurityUtils.requireCurrentUserId(principal)));
    }
}
