package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.CreateCommunityProfileRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityProfileRequest;
import com.carebridge.backend.community.dto.response.CommunityProfileResponse;
import com.carebridge.backend.community.service.CommunityProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-20 Create Community Profile / UC-21 Update Community Profile.
 * Controller only validates and delegates — no business logic here.
 */
@RestController
@RequestMapping("/api/v1/community/profiles")
@RequiredArgsConstructor
public class CommunityProfileController {

    private final CommunityProfileService profileService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommunityProfileResponse>> create(
            Principal principal,
            @Valid @RequestBody CreateCommunityProfileRequest request) {

        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        CommunityProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CommunityProfileResponse>> update(
            Principal principal,
            @Valid @RequestBody UpdateCommunityProfileRequest request) {

        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        CommunityProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
