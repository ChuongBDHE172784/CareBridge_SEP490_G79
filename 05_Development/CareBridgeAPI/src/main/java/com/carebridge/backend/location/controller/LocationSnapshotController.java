package com.carebridge.backend.location.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.location.dto.request.LocationSnapshotRequest;
import com.carebridge.backend.location.dto.response.LocationSnapshotResponse;
import com.carebridge.backend.location.service.ILocationSnapshotService;
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
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationSnapshotController {

    private final ILocationSnapshotService locationSnapshotService;

    @PostMapping("/snapshots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LocationSnapshotResponse>> createSnapshot(
            Principal principal,
            @Valid @RequestBody LocationSnapshotRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(locationSnapshotService.createSnapshot(userId, request)));
    }

    @GetMapping("/snapshots/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LocationSnapshotResponse>>> getMySnapshots(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(locationSnapshotService.getMySnapshots(userId)));
    }
}
