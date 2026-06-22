package com.carebridge.backend.consent.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.service.ConsentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consent/grants")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsentGrantResponse>> grant(
            Principal principal,
            @Valid @RequestBody GrantConsentRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(consentService.grantConsent(userId, request), "Consent granted"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsentGrantResponse>>> list(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(consentService.listConsents(userId)));
    }

    @DeleteMapping("/{consentId}")
    public ResponseEntity<ApiResponse<ConsentGrantResponse>> revoke(
            Principal principal,
            @PathVariable Long consentId) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(consentService.revokeConsent(userId, consentId), "Consent revoked"));
    }
}
