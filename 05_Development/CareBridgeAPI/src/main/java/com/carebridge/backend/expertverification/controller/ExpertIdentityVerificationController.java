package com.carebridge.backend.expertverification.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expertverification.dto.request.ReviewIdentityRequest;
import com.carebridge.backend.expertverification.dto.response.ExpertOnboardingResponse;
import com.carebridge.backend.expertverification.dto.response.IdentityVerificationResponse;
import com.carebridge.backend.expertverification.service.IExpertIdentityVerificationService;
import com.carebridge.backend.file.dto.ViewFileResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertIdentityVerificationController {

    private final IExpertIdentityVerificationService identityService;

    @GetMapping("/onboarding")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertOnboardingResponse>> getOnboarding(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(identityService.getOnboarding(userId)));
    }

    @PostMapping(value = "/identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> submitIdentity(
            Principal principal,
            @RequestPart("selfie") MultipartFile selfie,
            @RequestPart("identityFront") MultipartFile identityFront,
            @RequestPart("identityBack") MultipartFile identityBack) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                identityService.submit(userId, selfie, identityFront, identityBack)));
    }

    @GetMapping("/identity/files/{fileId}/url")
    @PreAuthorize("hasAnyRole('EXPERT', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ViewFileResponse>> getIdentityFileUrl(
            Principal principal, @PathVariable UUID fileId) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                identityService.getAuthorizedFileUrl(fileId, callerId)));
    }

    @GetMapping("/identity/pending")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>> getPendingReviews() {
        return ResponseEntity.ok(ApiResponse.success(identityService.getPendingReviews()));
    }

    @PutMapping("/identity/{attemptId}/review")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> review(
            Principal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody ReviewIdentityRequest request) {
        UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                identityService.review(attemptId, request, reviewerId)));
    }
}
