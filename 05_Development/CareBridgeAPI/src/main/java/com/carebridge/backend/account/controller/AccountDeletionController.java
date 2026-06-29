package com.carebridge.backend.account.controller;

import com.carebridge.backend.account.dto.request.RequestAccountDeletionRequest;
import com.carebridge.backend.account.dto.response.AccountDeletionRequestResponse;
import com.carebridge.backend.account.service.AccountDeletionService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account management endpoints")
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;

    @PostMapping("/deletion-request")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Request account deletion",
        description = "Submit a request to delete the authenticated user's account. A 30-day grace period applies before permanent deletion. Requires password confirmation."
    )
    public ResponseEntity<ApiResponse<AccountDeletionRequestResponse>> requestDeletion(
            Principal principal,
            @Valid @RequestBody RequestAccountDeletionRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        AccountDeletionRequestResponse response = accountDeletionService.requestDeletion(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Account deletion requested. Your account will be deleted after 30 days."));
    }

    @DeleteMapping("/deletion-request")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Cancel account deletion request",
        description = "Cancel a pending account deletion request. The account will remain active."
    )
    public ResponseEntity<ApiResponse<AccountDeletionRequestResponse>> cancelDeletion(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        AccountDeletionRequestResponse response = accountDeletionService.cancelDeletion(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Account deletion request cancelled"));
    }
}
