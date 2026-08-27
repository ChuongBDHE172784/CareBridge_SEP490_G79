package com.carebridge.backend.expertverification.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.dto.response.CredentialDocumentPreviewResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.expertverification.service.IExpertCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert/credentials")
@RequiredArgsConstructor
public class ExpertCredentialController {

  private final IExpertCredentialService credentialService;

  // UC-62: Submit credential with file upload
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('EXPERT')")
  public ResponseEntity<ApiResponse<CredentialResponse>> submitCredential(
    Principal principal,
    @Valid @ModelAttribute SubmitCredentialRequest request,
    @RequestPart("file") MultipartFile file) {
    UUID userId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(ApiResponse.success(credentialService.submitCredential(userId, request, file)));
  }

  // UC-63: Get my credentials
  @GetMapping("/me")
  @PreAuthorize("hasRole('EXPERT')")
  public ResponseEntity<ApiResponse<List<CredentialResponse>>> getMyCredentials(Principal principal) {
    UUID userId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(credentialService.getMyCredentials(userId)));
  }

  // Get credential detail
  @GetMapping("/{credentialId}")
  @PreAuthorize("hasRole('EXPERT')")
  public ResponseEntity<ApiResponse<CredentialResponse>> getCredentialDetail(
    Principal principal, @PathVariable UUID credentialId) {
    UUID userId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(credentialService.getCredentialDetail(credentialId, userId)));
  }

  // Delete credential
  @DeleteMapping("/{credentialId}")
  @PreAuthorize("hasRole('EXPERT')")
  public ResponseEntity<ApiResponse<Void>> deleteCredential(
    Principal principal, @PathVariable UUID credentialId) {
    UUID userId = SecurityUtils.requireCurrentUserId(principal);
    credentialService.deleteCredential(credentialId, userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  // UC-70: Admin review credential
  @PutMapping("/{credentialId}/review")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public ResponseEntity<ApiResponse<DocumentReviewResponse>> reviewCredential(
    Principal principal,
    @PathVariable UUID credentialId,
    @Valid @RequestBody ReviewCredentialRequest request) {
    UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(credentialService.reviewCredential(credentialId, request, reviewerId)));
  }

  // UC-70: Admin get pending reviews
  @GetMapping("/pending")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public ResponseEntity<ApiResponse<List<DocumentReviewResponse>>> getPendingReviews(
    Principal principal,
    @RequestParam(required = false) String credentialType) {
    UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(credentialService.getPendingReviews(credentialType, reviewerId)));
  }

  @GetMapping("/{credentialId}/preview")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public ResponseEntity<ApiResponse<CredentialDocumentPreviewResponse>> previewCredential(
      Principal principal, @PathVariable UUID credentialId) {
    UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(
        credentialService.previewCredential(credentialId, reviewerId)));
  }

  @GetMapping("/{credentialId}/file")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public ResponseEntity<ApiResponse<ViewFileResponse>> getCredentialFile(
      Principal principal, @PathVariable UUID credentialId) {
    UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(
        credentialService.getCredentialFile(credentialId, reviewerId)));
  }
}
