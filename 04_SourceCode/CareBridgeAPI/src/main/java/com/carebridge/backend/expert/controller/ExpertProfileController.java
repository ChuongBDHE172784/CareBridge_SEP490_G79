package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UploadCredentialRequest;
import com.carebridge.backend.expert.dto.response.ExpertCredentialResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.service.ExpertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertProfileController {

    private final ExpertService expertService;

    @PostMapping("/profile")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertProfileResponse> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateExpertProfileRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpertProfileResponse response = expertService.createProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertProfileResponse> getOwnProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpertProfileResponse response = expertService.getOwnProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateExpertProfileRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpertProfileResponse response = expertService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{expertId}")
    public ResponseEntity<ExpertProfilePublicResponse> getPublicProfile(
            @PathVariable UUID expertId
    ) {
        ExpertProfilePublicResponse response = expertService.getPublicProfile(expertId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verification/documents")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertCredentialResponse> uploadCredential(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute UploadCredentialRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpertCredentialResponse response = expertService.uploadCredential(userId, file, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verification/documents")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<List<ExpertCredentialResponse>> getMyCredentials(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<ExpertCredentialResponse> responses = expertService.getMyCredentials(userId);
        return ResponseEntity.ok(responses);
    }
}
