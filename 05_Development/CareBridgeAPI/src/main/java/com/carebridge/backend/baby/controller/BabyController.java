package com.carebridge.backend.baby.controller;

import com.carebridge.backend.baby.dto.ArchiveBabyProfileResponse;
import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.dto.UpdateBabyProfileRequest;
import com.carebridge.backend.baby.dto.UpdateBabyProfileResponse;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import com.carebridge.backend.baby.dto.LinkBabyJourneyRequest;
import com.carebridge.backend.baby.dto.LinkBabyJourneyResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.baby.security.BabyLinkBoundaryAuditFilter;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class BabyController {

    private final IBabyService babyService;

    // UC31: Create baby profile
    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<CreateBabyProfileResponse>> createBabyProfile(
            @Valid @RequestBody CreateBabyProfileRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.createBabyProfile(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Baby profile created successfully"));
    }

    // UC32: List baby profiles for the current user
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BabyProfileDetailResponse>>> listBabyProfiles(
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var list = babyService.listBabyProfiles(callerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // UC192: View baby profile
    @GetMapping("/{babyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BabyProfileDetailResponse>> getBabyProfile(
            @PathVariable UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.getBabyProfile(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC193: Switch active baby profile
    @PatchMapping("/{babyId}/active")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<BabyProfileDetailResponse>> switchActiveBabyProfile(
            @PathVariable UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.switchActiveBabyProfile(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Active baby profile switched successfully"));
    }

    // UC32: Update baby profile
    @PutMapping("/{babyId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<UpdateBabyProfileResponse>> updateBabyProfile(
            @PathVariable UUID babyId,
            @Valid @RequestBody UpdateBabyProfileRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.updateBabyProfile(babyId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC33: Archive baby profile
    @PostMapping("/{babyId}/archive")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ArchiveBabyProfileResponse>> archiveBabyProfile(
            @PathVariable UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.archiveBabyProfile(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{babyId}/journey-link")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<LinkBabyJourneyResponse>> linkExistingBaby(
            @PathVariable UUID babyId, @Valid @RequestBody LinkBabyJourneyRequest request,
            Principal principal, HttpServletRequest httpRequest) {
        BabyLinkBoundaryAuditFilter.markControllerEntered(httpRequest);
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(babyService.linkExistingBaby(babyId, request, callerId)));
    }

}
