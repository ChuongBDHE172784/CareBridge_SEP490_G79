package com.carebridge.backend.identity.admin.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.admin.dto.request.CreateStaffAccountRequest;
import com.carebridge.backend.identity.admin.dto.response.StaffAccountResponse;
import com.carebridge.backend.identity.admin.service.AdminStaffService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC115 Create Staff Account — admin-only provisioning endpoint. ADR-IAM-004:
 * only SYSTEM_ADMIN may ever reach this controller method (no role-hierarchy
 * delegation) — structurally eliminates self-escalation.
 */
@RestController
@RequestMapping("/api/v1/admin/staff-accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffAccountResponse>> createStaffAccount(
            @Valid @RequestBody CreateStaffAccountRequest request,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        StaffAccountResponse response = adminStaffService.createStaffAccount(callerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Staff account created; temporary credentials sent by email"));
    }
}
