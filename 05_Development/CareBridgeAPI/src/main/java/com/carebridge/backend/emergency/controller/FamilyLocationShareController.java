package com.carebridge.backend.emergency.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.emergency.dto.request.ShareLocationRequest;
import com.carebridge.backend.emergency.dto.response.LocationShareResponse;
import com.carebridge.backend.emergency.service.FamilyLocationShareService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergency/location-shares")
@RequiredArgsConstructor
public class FamilyLocationShareController {

    private final FamilyLocationShareService locationShareService;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<LocationShareResponse>> share(
            @Valid @RequestBody ShareLocationRequest request,
            Principal principal) {
        UUID motherId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(locationShareService.share(motherId, request)));
    }
}
