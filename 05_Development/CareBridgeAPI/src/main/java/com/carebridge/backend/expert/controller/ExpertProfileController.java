package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.service.IExpertProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expert-profiles")
public class ExpertProfileController {

    private final IExpertProfileService expertProfileService;

    public ExpertProfileController(IExpertProfileService expertProfileService) {
        this.expertProfileService = expertProfileService;
    }

    @PreAuthorize("hasRole('EXPERT')")
    @PostMapping
    public ResponseEntity<ExpertProfileResponse> createProfile(
            @Valid @RequestBody CreateExpertProfileRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        ExpertProfileResponse response = expertProfileService.createProfile(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
