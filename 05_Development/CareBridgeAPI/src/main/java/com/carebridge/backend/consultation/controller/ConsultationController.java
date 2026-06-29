package com.carebridge.backend.consultation.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.request.UpdateConsultationStatusRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationDetailDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.service.BookingServiceImpl;
import com.carebridge.backend.consultation.service.ConsultationServiceImpl;
import com.carebridge.backend.security.annotation.RequireRoles;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consultation Controller.
 * REST endpoints for consultation booking and management.
 *
 * TV4 API Spec: /api/v1/consultations
 */
@RestController
@RequestMapping("/api/v1/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final IBookingService bookingService;
    private final IConsultationService consultationService;

    /**
     * POST /api/v1/consultations/book
     * Book a consultation with an expert (P0).
     *
     * Auth: USER (MOTHER/FAMILY)
     */
    @PostMapping("/book")
    @RequireRoles({Role.MOTHER, Role.FAMILY})
    public ResponseEntity<ApiResponse<BookingResponse>> bookConsultation(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        BookingResponse response = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking created successfully"));
    }

    /**
     * GET /api/v1/consultations/{bookingId}
     * Get consultation detail (P1).
     *
     * Auth: Requester, Expert, or Admin
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<ConsultationDetailDTO>> getConsultation(
            @PathVariable("bookingId") Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userDetails != null ? Long.valueOf(userDetails.getUsername()) : null;
        String roleStr = userDetails != null ?
                userDetails.getAuthorities().iterator().next().getAuthority() : "ANONYMOUS";
        var role = Role.valueOf(roleStr);

        ConsultationDetailDTO response = consultationService.getConsultation(bookingId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/consultations/my
     * Get user's own consultations (P2).
     *
     * Auth: Any authenticated user
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ConsultationSummaryDTO>>> getMyConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.valueOf(userDetails.getUsername());
        String roleStr = userDetails.getAuthorities().iterator().next().getAuthority();
        var role = Role.valueOf(roleStr);

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledStart").descending());
        Page<ConsultationSummaryDTO> consultations = consultationService.getUserConsultations(userId, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(consultations));
    }

    /**
     * PUT /api/v1/consultations/{bookingId}/status
     * Update consultation status (cancel/reschedule) (P2).
     *
     * Auth: Requester, Expert, or Admin
     */
    @PutMapping("/{bookingId}/status")
    @RequireRoles({Role.MOTHER, Role.FAMILY, Role.EXPERT, Role.SYSTEM_ADMIN})
    public ResponseEntity<ApiResponse<Void>> updateConsultationStatus(
            @PathVariable("bookingId") Long bookingId,
            @Valid @RequestBody UpdateConsultationStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        String roleStr = userDetails.getAuthorities().iterator().next().getAuthority();
        var role = Role.valueOf(roleStr);

        if (request.getStatus() == com.carebridge.backend.expert.enums.ConsultationStatus.CANCELLED) {
            consultationService.cancelConsultation(bookingId, userId, role, request.getReason());
        } else {
            // TODO: Implement rescheduling logic in Sprint 1+
            throw new UnsupportedOperationException("Rescheduling not supported in Sprint 0");
        }

        return ResponseEntity.ok(ApiResponse.success(null, "Consultation status updated"));
    }
}
