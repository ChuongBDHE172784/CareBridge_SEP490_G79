package com.carebridge.backend.baby.controller;

import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class BabyController {

    private final IBabyService babyService;

    // UC31: Create baby profile
    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY_MEMBER')")
    public ResponseEntity<ApiResponse<CreateBabyProfileResponse>> createBabyProfile(
            @Valid @RequestBody CreateBabyProfileRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = babyService.createBabyProfile(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Baby profile created successfully"));
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
}
