package com.carebridge.backend.identity.admin.controller;

import com.carebridge.backend.common.constants.AppConstants;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.admin.dto.request.AdminUserSearchQuery;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserActivityResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSessionResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.service.AdminUserService;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC114 Manage User Accounts — admin-only endpoints. Controller does validation +
 * mapping only; all business logic (including the self-target guard) lives in
 * {@link AdminUserService}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminUserSummaryResponse>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AdminUserSearchQuery query = new AdminUserSearchQuery();
        query.setEmail(email);
        query.setPhone(phone);
        query.setName(name);
        query.setRole(role);
        query.setEnabled(enabled);
        query.setLocked(locked);

        int pageSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.of(adminUserService.searchUsers(query, pageable)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(userId)));
    }

    @GetMapping("/{userId}/sessions")
    public ResponseEntity<PaginatedResponse<AdminUserSessionResponse>> getUserSessions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.of(adminUserService.getUserSessions(userId, pageable)));
    }

    @GetMapping("/{userId}/activity")
    public ResponseEntity<PaginatedResponse<AdminUserActivityResponse>> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.of(adminUserService.getUserActivity(userId, pageable)));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        AdminUserSummaryResponse response = adminUserService.updateStatus(callerId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Account status updated"));
    }
}
