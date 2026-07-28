package com.carebridge.backend.security.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.identity.admin.dto.request.SubmitAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.response.AccountLockAppealResponse;
import com.carebridge.backend.identity.admin.service.AccountLockAppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/lock-appeals")
@RequiredArgsConstructor
public class AccountLockAppealController {
    private final AccountLockAppealService appealService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountLockAppealResponse>> submit(
            @Valid @RequestBody SubmitAccountLockAppealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appealService.submit(request), "Appeal submitted"));
    }
}
