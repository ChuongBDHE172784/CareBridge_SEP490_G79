package com.carebridge.backend.identity.admin.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserRoleRequest;
import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import com.carebridge.backend.identity.admin.service.AdminRoleService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC116 Update Role and Permission — the single highest-risk endpoint in the Admin
 * Governance cluster. ADR-IAM-007: only SYSTEM_ADMIN may call this, and even a
 * SYSTEM_ADMIN can never target their own account (unconditional self-target guard
 * lives in AdminRoleServiceImpl, not here).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserRoleResponse>> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        UserRoleResponse response = adminRoleService.updateRole(callerId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Role updated"));
    }
}
