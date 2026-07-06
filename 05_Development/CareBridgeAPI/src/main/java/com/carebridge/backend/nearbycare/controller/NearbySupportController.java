package com.carebridge.backend.nearbycare.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.nearbycare.dto.request.CreateNearbySupportRequest;
import com.carebridge.backend.nearbycare.dto.request.RespondSupportRequest;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportRequestResponse;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportResponseResponse;
import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import com.carebridge.backend.nearbycare.service.INearbySupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nearbycare")
@RequiredArgsConstructor
public class NearbySupportController {

    private final INearbySupportService nearbySupportService;
    private final ExpertProfileRepository expertProfileRepository;

    // UC-81: Mother creates nearby support request
    @PostMapping("/support-requests")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<NearbySupportRequestResponse>> createRequest(
            Principal principal,
            @Valid @RequestBody CreateNearbySupportRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        NearbySupportRequest entity = nearbySupportService.createRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        new com.carebridge.backend.nearbycare.mapper.NearbySupportMapper().toRequestResponse(entity)));
    }

    // UC-81: Mother views own requests
    @GetMapping("/support-requests/me")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<NearbySupportRequestResponse>>> getMyRequests(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        List<NearbySupportRequest> requests = nearbySupportService.getMyRequests(userId);
        return ResponseEntity.ok(ApiResponse.success(
                requests.stream()
                        .map(r -> new com.carebridge.backend.nearbycare.mapper.NearbySupportMapper().toRequestResponse(r))
                        .toList()));
    }

    // UC-81: Mother cancels request
    @DeleteMapping("/support-requests/{requestId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            Principal principal,
            @PathVariable UUID requestId) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        nearbySupportService.cancelRequest(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // UC-82: Expert views open nearby support requests
    @GetMapping("/support-requests/open")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<List<NearbySupportRequestResponse>>> getOpenRequests(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        UUID expertProfileId = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-004", "Expert profile not found"))
                .getExpertProfileId();

        List<NearbySupportRequest> openRequests = nearbySupportService.getOpenRequests();
        return ResponseEntity.ok(ApiResponse.success(
                openRequests.stream()
                        .map(r -> new com.carebridge.backend.nearbycare.mapper.NearbySupportMapper().toRequestResponse(r))
                        .toList()));
    }

    // UC-82: Expert responds to a request (ACCEPT/DECLINE/STOP)
    @PostMapping("/support-requests/{requestId}/respond")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<NearbySupportResponseResponse>> respondToRequest(
            Principal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody RespondSupportRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        UUID expertProfileId = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-004", "Expert profile not found"))
                .getExpertProfileId();

        var response = nearbySupportService.respondToRequest(requestId, expertProfileId, request);
        return ResponseEntity.ok(ApiResponse.success(
                new com.carebridge.backend.nearbycare.mapper.NearbySupportMapper().toResponseResponse(response)));
    }
}
