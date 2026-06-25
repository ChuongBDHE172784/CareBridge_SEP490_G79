package com.carebridge.backend.partner.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.service.PartnerProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partner/profile")
@PreAuthorize("hasRole('PARTNER')")
@RequiredArgsConstructor
public class PartnerProfileController {

    private final PartnerProfileService partnerProfileService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePartnerProfileResponse>> createProfile(
            @Valid @RequestBody CreatePartnerProfileRequest request,
            Principal principal) {
        // Oracle: ADR-002 — actorId MUST come from SecurityContext, never from request body
        java.util.UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        CreatePartnerProfileResponse response = partnerProfileService.createProfile(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Partner profile created successfully"));
    }
}
