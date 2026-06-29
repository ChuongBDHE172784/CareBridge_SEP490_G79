package com.carebridge.backend.expert.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.expert.dto.request.CreateExpertRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertRequest;
import com.carebridge.backend.expert.dto.request.UploadVerificationDocumentRequest;
import com.carebridge.backend.expert.dto.response.AvailabilitySlotDTO;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertReviewDTO;
import com.carebridge.backend.expert.entity.AvailabilitySlot;
import com.carebridge.backend.expert.service.IAvailabilityService;
import com.carebridge.backend.expert.service.IExpertService;
import com.carebridge.backend.expert.policy.ExpertPolicy;
import com.carebridge.backend.security.annotation.RequireRoles;
import com.carebridge.backend.security.rbac.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Expert Controller.
 * REST endpoints for expert profile management and related operations.
 *
 * TV4 API Spec: /api/v1/experts
 */
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final IExpertService expertService;
    private final IAvailabilityService availabilityService;
    private final ExpertPolicy expertPolicy;

    /**
     * POST /api/v1/experts
     * Create expert profile (P0).
     *
     * Auth: VERIFIED_EXPERT
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRoles(Role.EXPERT)
    public ResponseEntity<ApiResponse<ExpertProfilePublicResponse>> createExpert(
            @Valid @ModelAttribute CreateExpertRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long userId = Long.valueOf(userDetails.getUsername()); // Assuming username is userId
        ExpertProfilePublicResponse response = expertService.createExpertProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Expert profile created successfully"));
    }

    /**
     * GET /api/v1/experts/{id}
     * View expert profile (public view) (P0).
     *
     * Auth: USER (any authenticated user)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpertProfilePublicResponse>> getExpertProfile(
            @PathVariable("id") Long expertId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long requestingUserId = userDetails != null ? Long.valueOf(userDetails.getUsername()) : null;
        String role = userDetails != null ? userDetails.getAuthorities().iterator().next().getAuthority() : "ANONYMOUS";

        ExpertProfilePublicResponse response = expertService.getExpertProfile(
                expertId, requestingUserId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/experts/{id}/detail
     * Get expert profile detail (owner or admin only) (P2).
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> getExpertProfileDetail(
            @PathVariable("id") Long expertId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long requestingUserId = userDetails != null ? Long.valueOf(userDetails.getUsername()) : null;
        String role = userDetails != null ? userDetails.getAuthorities().iterator().next().getAuthority() : "ANONYMOUS";

        ExpertProfileDetailResponse response = expertService.getExpertProfileDetail(
                expertId, requestingUserId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/experts/{id}
     * Update expert profile (P0).
     *
     * Auth: EXPERT (own) or ADMIN
     */
    @PutMapping("/{id}")
    @RequireRoles({Role.EXPERT, Role.SYSTEM_ADMIN})
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> updateExpertProfile(
            @PathVariable("id") Long expertId,
            @Valid @RequestBody UpdateExpertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        ExpertProfileDetailResponse response = expertService.updateExpertProfile(
                expertId, userId, role, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Expert profile updated"));
    }

    /**
     * POST /api/v1/experts/{id}/availability
     * Configure availability slots (P0).
     *
     * Auth: EXPERT (own) or ADMIN
     */
    @PostMapping("/{id}/availability")
    @RequireRoles({Role.EXPERT, Role.SYSTEM_ADMIN})
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> configureAvailability(
            @PathVariable("id") Long expertId,
            @Valid @RequestBody com.carebridge.backend.expert.dto.request.ConfigureAvailabilityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        String roleStr = userDetails.getAuthorities().iterator().next().getAuthority();
        var role = Role.valueOf(roleStr);

        // RBAC: Verify user can configure availability for this expert
        Expert expert = expertService.getExpertEntity(expertId); // Need to add this method or load expert
        expertPolicy.ensureCanConfigureAvailability(expert, userId, role);

        // Convert DTOs to entities
        List<AvailabilitySlot> slots = request.getSlots().stream()
                .map(slotDto -> AvailabilitySlot.builder()
                        .expertId(expertId)
                        .slotStart(slotDto.getSlotStart())
                        .slotEnd(slotDto.getSlotEnd())
                        .channelType(slotDto.getChannelType())
                        .status(slotDto.getStatus())
                        .build())
                .toList();

        List<AvailabilitySlot> createdSlots = availabilityService.configureSlots(expertId, slots);

        List<AvailabilitySlotDTO> slotDTOs = createdSlots.stream()
                .map(availabilityService::toSlotDTO)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(slotDTOs, "Availability configured"));
    }

    /**
     * GET /api/v1/experts/{id}/availability
     * Get expert availability slots (P2).
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailability(
            @PathVariable("id") Long expertId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        List<AvailabilitySlot> slots;
        if (from != null && to != null) {
            slots = availabilityService.getAvailableSlots(expertId, from, to);
        } else {
            // Default to next 7 days
            Instant now = Instant.now();
            Instant weekFromNow = now.plus(7, java.time.temporal.ChronoUnit.DAYS);
            slots = availabilityService.getAvailableSlots(expertId, now, weekFromNow);
        }

        List<AvailabilitySlotDTO> slotDTOs = slots.stream()
                .map(availabilityService::toSlotDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(slotDTOs));
    }

    /**
     * GET /api/v1/experts/{id}/reviews
     * Get expert reviews (P2).
     */
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ExpertReviewDTO>>> getExpertReviews(
            @PathVariable("id") Long expertId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long requestingUserId = userDetails != null ? Long.valueOf(userDetails.getUsername()) : null;
        String role = userDetails != null ? userDetails.getAuthorities().iterator().next().getAuthority() : "ANONYMOUS";

        List<ExpertReviewDTO> reviews = expertService.getExpertReviews(expertId, requestingUserId, role);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * GET /api/v1/experts
     * Search experts (P0).
     *
     * Query params: keyword, specialty, minRating, verifiedOnly, page, size
     */
    @GetMapping
    public ResponseEntity<ApiResponse<com.carebridge.backend.consultation.dto.response.ExpertSearchResultDTO>> searchExperts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "true") Boolean verifiedOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // TODO: Implement search logic
        // For Sprint 0 MVP, return empty result with stub
        return ResponseEntity.ok(ApiResponse.success(
                com.carebridge.backend.consultation.dto.response.ExpertSearchResultDTO.builder()
                        .experts(List.of())
                        .total(0)
                        .page(page)
                        .size(size)
                        .totalPages(0)
                        .build()
        ));
    }
}
