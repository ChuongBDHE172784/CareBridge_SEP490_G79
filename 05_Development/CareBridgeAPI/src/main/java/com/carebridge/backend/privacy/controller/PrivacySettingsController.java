package com.carebridge.backend.privacy.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.privacy.dto.PrivacySettingsResponse;
import com.carebridge.backend.privacy.dto.UpdatePrivacySettingsRequest;
import com.carebridge.backend.privacy.service.PrivacySettingsService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy-settings")
@RequiredArgsConstructor
public class PrivacySettingsController {

    private final PrivacySettingsService privacySettingsService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> getMySettings(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        PrivacySettingsResponse response = privacySettingsService.getSettings(userId, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> updateMySettings(
            @Valid @RequestBody UpdatePrivacySettingsRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        PrivacySettingsResponse response = privacySettingsService.updateSettings(userId, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Privacy settings updated successfully"));
    }
}
