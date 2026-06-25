package com.carebridge.backend.payment.service;

import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.payment.dto.request.ProcessPaymentRequest;
import com.carebridge.backend.payment.dto.request.RefundRequest;
import com.carebridge.backend.payment.dto.response.PaymentResponse;
import com.carebridge.backend.payment.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Payment Service Interface.
 * Defines contract for payment processing operations.
 *
 * ISP: Interface segregation - only payment-specific methods.
 */
public interface IPaymentService {

    /**
     * Process payment for a consultation booking.
     *
     * @param bookingId the booking ID
     * @param userId the user making payment
     * @param request the payment request
     * @return payment response with transaction details
     */
    PaymentResponse processPayment(Long bookingId, Long userId, ProcessPaymentRequest request);

    /**
     * Get payment status by booking reference.
     *
     * @param bookingRef the booking reference
     * @return payment transaction details
     */
    PaymentResponse getPaymentStatus(String bookingRef);

    /**
     * Get payment transaction by ID.
     *
     * @param paymentId the payment ID
     * @return payment transaction
     */
    PaymentTransaction getPaymentTransaction(Long paymentId);

    /**
     * Get payments for a consultation.
     *
     * @param bookingId the booking ID
     * @return list of payments
     */
    List<PaymentTransaction> getPaymentsByConsultation(Long bookingId);

    /**
     * Get payments by user with pagination.
     *
     * @param userId the user ID
     * @param role the user's role
     * @param pageable pagination parameters
     * @return paged payment results
     */
    Page<PaymentTransaction> getPaymentsByUser(Long userId, String role, Pageable pageable);

    /**
     * Issue a refund for a payment.
     *
     * @param paymentId the payment ID
     * @param request the refund request
     * @return refund record
     */
    com.carebridge.backend.payment.entity.RefundRecord issueRefund(Long paymentId, RefundRequest request);
}
