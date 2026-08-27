package com.carebridge.backend.expertavailability.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.request.SetOnlineStatusRequest;
import com.carebridge.backend.expertavailability.dto.request.ReplaceAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.request.ShareLocationRequest;
import com.carebridge.backend.expertavailability.dto.response.AvailabilityResponse;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.service.IExpertAvailabilityService;
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
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertAvailabilityController {

private final IExpertAvailabilityService availabilityService;
private final ExpertProfileRepository expertProfileRepository;

private UUID resolveExpertProfileId(UUID userId) {
return expertProfileRepository.findByUserId(userId)
.orElseThrow(() -> new ExpertException(
org.springframework.http.HttpStatus.NOT_FOUND,
"EXPERT-004", "Expert profile not found"))
.getExpertProfileId();
}

@PostMapping("/availability")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<AvailabilityResponse>> createAvailability(
Principal principal, @Valid @RequestBody CreateAvailabilityRequest request) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
return ResponseEntity.status(HttpStatus.CREATED)
.body(ApiResponse.success(availabilityService.createAvailability(expertProfileId, request)));
}

@GetMapping("/availability/me")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getMyAvailability(Principal principal) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
return ResponseEntity.ok(ApiResponse.success(availabilityService.getMyAvailability(expertProfileId)));
}

@GetMapping("/availability/{expertProfileId}")
@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getPublicAvailability(
        @PathVariable UUID expertProfileId) {
return ResponseEntity.ok(ApiResponse.success(
        availabilityService.getPublicAvailability(expertProfileId)));
}

@PutMapping("/availability/batch")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> replaceAvailability(
        Principal principal, @Valid @RequestBody ReplaceAvailabilityRequest request) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
return ResponseEntity.ok(ApiResponse.success(
        availabilityService.replaceAvailability(expertProfileId, request)));
}

@DeleteMapping("/availability/{id}")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<Void>> deleteAvailability(Principal principal, @PathVariable UUID id) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
availabilityService.deleteAvailability(id, expertProfileId);
return ResponseEntity.ok(ApiResponse.success(null));
}

@PostMapping("/location/share")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<LocationShareResponse>> shareLocation(
Principal principal, @Valid @RequestBody ShareLocationRequest request) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
return ResponseEntity.ok(ApiResponse.success(availabilityService.shareLocation(expertProfileId, request)));
}

@PatchMapping("/online-status")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<LocationShareResponse>> setOnlineStatus(
Principal principal, @Valid @RequestBody SetOnlineStatusRequest request) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
return ResponseEntity.ok(ApiResponse.success(
availabilityService.setOnlineStatus(expertProfileId, request.getOnline())));
}

@DeleteMapping("/location/share")
@PreAuthorize("hasRole('EXPERT')")
public ResponseEntity<ApiResponse<Void>> stopLocationShare(Principal principal) {
UUID userId = SecurityUtils.requireCurrentUserId(principal);
UUID expertProfileId = resolveExpertProfileId(userId);
availabilityService.stopLocationShare(expertProfileId);
return ResponseEntity.ok(ApiResponse.success(null));
}
}
