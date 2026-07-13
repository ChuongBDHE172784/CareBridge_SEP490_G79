package com.carebridge.backend.emergency.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.emergency.dto.request.EmergencyContactRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyContactResponse;
import com.carebridge.backend.emergency.service.IEmergencyContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency/contact")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final IEmergencyContactService emergencyContactService;

    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> getContact(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(emergencyContactService.getContact(userId)));
    }

    @PutMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> upsertContact(
            @Valid @RequestBody EmergencyContactRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(emergencyContactService.upsertContact(userId, request)));
    }
}
