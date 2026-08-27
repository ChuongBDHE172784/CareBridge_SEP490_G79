package com.carebridge.backend.identity.admin.controller;

import com.carebridge.backend.common.constants.AppConstants;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.admin.dto.request.ReviewAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.response.AccountLockAppealResponse;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import com.carebridge.backend.identity.admin.service.AccountLockAppealService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/account-lock-appeals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminAccountLockAppealController {
    private final AccountLockAppealService appealService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<AccountLockAppealResponse>> list(
            @RequestParam(required = false) AccountLockAppealStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.of(appealService.list(status,
                PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE)))));
    }

    @GetMapping("/{appealId}")
    public ResponseEntity<ApiResponse<AccountLockAppealResponse>> get(@PathVariable UUID appealId) {
        return ResponseEntity.ok(ApiResponse.success(appealService.get(appealId)));
    }

    @PatchMapping("/{appealId}/review")
    public ResponseEntity<ApiResponse<AccountLockAppealResponse>> review(
            @PathVariable UUID appealId,
            @Valid @RequestBody ReviewAccountLockAppealRequest request,
            Principal principal) {
        UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                appealService.review(reviewerId, appealId, request), "Appeal reviewed"));
    }
}

