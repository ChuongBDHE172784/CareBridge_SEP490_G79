package com.carebridge.backend.profile.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.profile.dto.ProfileResponse;
import com.carebridge.backend.profile.dto.UpdateProfileRequest;
import com.carebridge.backend.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ApiResponse.success(profileService.getProfile(userId));
    }

    @PatchMapping
    public ApiResponse<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ApiResponse.success(response, "Profile updated successfully");
    }
}
