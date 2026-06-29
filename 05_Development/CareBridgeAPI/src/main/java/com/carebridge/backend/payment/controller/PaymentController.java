package com.carebridge.backend.payment.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.repository.ConsultationRepository;
import com.carebridge.backend.payment.dto.request.ProcessPaymentRequest;
import com.carebridge.backend.payment.dto.request.RefundRequest;
import com.carebridge.backend.payment.dto.response.PaymentResponse;
import com.carebridge.backend.payment.service.IPaymentService;
import com.carebridge.backend.security.annotation.RequireRoles;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment Controller.
 * REST endpoints for payment processing.
 *
 * TV4 API Spec: /api/v1/payments
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;
    private final ConsultationRepository consultationRepository;

    /**
     * POST /api/v1/payments/process
     * Process payment for consultation (P0).
     *
     * Auth: Any authenticated user (booking requester)
     */
    @PostMapping("/process")
    @RequireRoles({Role.MOTHER, Role.FAMILY})
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());

        // Find consultation by booking reference
        Consultation consultation = consultationRepository.findByBookingRef(request.getBookingRef())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        PaymentResponse response = paymentService.processPayment(
                consultation.getBookingId(), userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment processed successfully"));
    }

    /**
     * GET /api/v1/payments/{bookingRef}/status
     * Get payment status for a booking (P1).
     *
     * Uses booking reference instead of consultationId for client convenience.
     */
    @GetMapping("/{bookingRef}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable("bookingRef") String bookingRef,
            @AuthenticationPrincipal UserDetails userDetails) {

        PaymentResponse response = paymentService.getPaymentStatus(bookingRef);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/payments/refund
     * Issue a refund for a payment (P2).
     *
     * Auth: SYSTEM_ADMIN only
     */
    @PostMapping("/refund")
    @RequireRoles(Role.SYSTEM_ADMIN)
    public ResponseEntity<ApiResponse<com.carebridge.backend.payment.entity.RefundRecord>> issueRefund(
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        com.carebridge.backend.payment.entity.RefundRecord refund = paymentService.issueRefund(
                request.getPaymentId(), request);
        return ResponseEntity.ok(ApiResponse.success(refund, "Refund initiated successfully"));
    }

    /**
     * GET /api/v1/payments/my
     * Get user's payment history (P2).
     *
     * Auth: Any authenticated user
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<com.carebridge.backend.payment.entity.PaymentTransaction>>> getMyPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.valueOf(userDetails.getUsername());
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        // Simple pagination - use Spring Data Pageable in production
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var paymentsPage = paymentService.getPaymentsByUser(userId, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(paymentsPage.getContent()));
    }
}
