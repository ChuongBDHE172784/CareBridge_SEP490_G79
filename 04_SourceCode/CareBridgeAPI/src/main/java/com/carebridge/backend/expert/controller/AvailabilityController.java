package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.request.AvailabilitySlotRequest;
import com.carebridge.backend.expert.dto.response.AvailabilitySlotResponse;
import com.carebridge.backend.expert.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getMyAvailability(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<AvailabilitySlotResponse> response = availabilityService.getMyAvailability(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<AvailabilitySlotResponse> createSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AvailabilitySlotRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        AvailabilitySlotResponse response = availabilityService.createSlot(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<AvailabilitySlotResponse> updateSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AvailabilitySlotRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        AvailabilitySlotResponse response = availabilityService.updateSlot(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<Void> deleteSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        availabilityService.deleteSlot(userId, id);
        return ResponseEntity.noContent().build();
    }
}
