package com.carebridge.backend.expert.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.RejectExpertRequest;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.VerificationStatusResponse;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.service.IExpertProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertProfileController {

    private final IExpertProfileService expertProfileService;

    // UC-60: Expert creates profile
    @PostMapping("/profiles")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> createProfile(
            @Valid @RequestBody CreateExpertProfileRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(expertProfileService.createProfile(userId, request)));
    }

    // UC-61: Expert views own profile
    @GetMapping("/profiles/me")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> getMyProfile(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.getMyProfile(userId)));
    }

    // UC-61: Expert updates own profile
    @PatchMapping("/profiles/me")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> updateProfile(
            @Valid @RequestBody UpdateExpertProfileRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.updateProfile(userId, request)));
    }

    // UC-65 / ADR-MEDI-001: Public directory of verified experts, now with optional text search.
    @GetMapping("/directory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertDirectoryResponse>> getDirectory(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.getPublicDirectory(specialty, q, page, size)));
    }

    // UC-65: Public profile view
    @GetMapping("/profiles/{expertProfileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> getPublicProfile(
            @PathVariable UUID expertProfileId) {
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.getPublicProfile(expertProfileId)));
    }

    // UC-70: Admin approves expert
    @PostMapping("/profiles/{expertProfileId}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveExpert(
            @PathVariable UUID expertProfileId,
            Principal principal) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        expertProfileService.approveExpert(expertProfileId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expert approved"));
    }

    // UC-70: Admin rejects expert
    @PostMapping("/profiles/{expertProfileId}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectExpert(
            @PathVariable UUID expertProfileId,
            Principal principal,
            @Valid @RequestBody RejectExpertRequest request) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        expertProfileService.rejectExpert(expertProfileId, adminId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Expert rejected"));
    }

    // UC-63: View verification status
    @GetMapping("/profiles/me/verification-status")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getMyVerificationStatus(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.getMyVerificationStatus(userId)));
    }

    // UC-63: Renew verification submission
    @PostMapping("/profiles/me/renew")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<Void>> renewVerification(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        expertProfileService.renewVerification(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi yêu cầu gia hạn"));
    }

    // UC-71: Admin set trust status
    @PatchMapping("/profiles/{expertProfileId}/trust")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setTrustStatus(
            @PathVariable UUID expertProfileId,
            Principal principal,
            @RequestParam TrustStatus status) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        expertProfileService.setTrustStatus(expertProfileId, status, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Trạng thái đã cập nhật"));
    }

    // Helper: get list of verified experts (for integration)
    @GetMapping("/verified")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ExpertProfileResponse>>> getVerifiedExperts() {
        return ResponseEntity.ok(ApiResponse.success(expertProfileService.getVerifiedExperts()));
    }
}
