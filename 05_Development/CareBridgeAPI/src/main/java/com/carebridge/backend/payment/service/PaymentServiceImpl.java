package com.carebridge.backend.payment.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.repository.ConsultationRepository;
import com.carebridge.backend.expert.enums.PaymentStatus;
import com.carebridge.backend.expert.policy.ExpertPolicy;
import com.carebridge.backend.payment.dto.request.ProcessPaymentRequest;
import com.carebridge.backend.payment.dto.request.RefundRequest;
import com.carebridge.backend.payment.dto.response.PaymentResponse;
import com.carebridge.backend.payment.entity.CommissionRecord;
import com.carebridge.backend.payment.entity.PaymentTransaction;
import com.carebridge.backend.payment.entity.RefundRecord;
import com.carebridge.backend.payment.mapper.PaymentMapper;
import com.carebridge.backend.payment.policy.PaymentPolicy;
import com.carebridge.backend.payment.repository.CommissionRecordRepository;
import com.carebridge.backend.payment.repository.PaymentTransactionRepository;
import com.carebridge.backend.payment.repository.RefundRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Service Implementation.
 * Handles payment processing and transaction management.
 *
 * Implements IPaymentService for payment operations.
 * Uses mock payment provider for Sprint 0.
 *
 * TV4 Use Cases: 3.1.2.1 (Process Payment Transaction)
 */
@Service("paymentService")
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final ConsultationRepository consultationRepository;
    private final CommissionRecordRepository commissionRepository;
    private final RefundRecordRepository refundRepository;
    private final PaymentPolicy paymentPolicy;
    private final ExpertPolicy expertPolicy;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse processPayment(Long bookingId, Long userId, ProcessPaymentRequest request) {
        log.info("Processing payment for bookingId: {}, userId: {}", bookingId, userId);

        // Find consultation
        Consultation consultation = consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Validate booking reference matches if provided
        if (request.getBookingRef() != null && !request.getBookingRef().equals(consultation.getBookingRef())) {
            throw new IllegalArgumentException("Booking reference mismatch");
        }

        // Check permissions
        paymentPolicy.ensureCanProcessPayment(consultation, userId,
                consultation.getRequesterUserId().equals(userId) ?
                        com.carebridge.backend.security.rbac.Role.MOTHER : com.carebridge.backend.security.rbac.Role.EXPERT);

        // Check consultation status
        if (consultation.getStatus() != com.carebridge.backend.expert.enums.ConsultationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Consultation is not in PENDING_PAYMENT status");
        }

        // Check if already paid
        Optional<PaymentTransaction> existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            PaymentTransaction existingPayment = existing.get();
            if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                throw new ResourceAlreadyExistsException("Payment already completed for this booking");
            }
            return paymentMapper.toPaymentResponse(existingPayment, consultation.getSessionToken());
        }

        // Mock VNPay processing
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            return processMockVNPay(consultation, request.getTransactionToken());
        }

        throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
    }

    /**
     * Mock VNPay payment processing.
     */
    private PaymentResponse processMockVNPay(Consultation consultation, String transactionToken) {
        log.debug("Processing mock VNPay payment for consultation: {}", consultation.getBookingRef());

        // Create payment transaction
        PaymentTransaction payment = PaymentTransaction.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .payerUserId(consultation.getRequesterUserId())
                .gatewayName("VNPAY_MOCK")
                .gatewayTransactionId("TXN-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .grossAmount(consultation.getPriceSnapshotAmount())
                .gatewayFee(calculateMockFee(consultation.getPriceSnapshotAmount()))
                .netPaidAmount(consultation.getPriceSnapshotAmount() - calculateMockFee(consultation.getPriceSnapshotAmount()))
                .currency(consultation.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);

        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Complete payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        // Update consultation status
        consultation.setStatus(com.carebridge.backend.expert.enums.ConsultationStatus.CONFIRMED);
        consultation.setPaymentTransactionId(payment.getPaymentId());
        consultationRepository.save(consultation);

        // Create commission record
        createCommissionRecord(payment, consultation);

        log.info("Payment completed: transactionId={}, bookingRef={}",
                payment.getGatewayTransactionId(), consultation.getBookingRef());

        // Generate session token for realtime
        String sessionToken = "session-token-" + UUID.randomUUID();
        consultation.setSessionToken(sessionToken);
        consultationRepository.save(consultation);

        return paymentMapper.toPaymentResponse(payment, sessionToken);
    }

    /**
     * Calculate mock VNPay fee (2% of transaction, min 2000 VND).
     */
    private int calculateMockFee(int grossAmount) {
        BigDecimal fee = BigDecimal.valueOf(grossAmount)
                .multiply(BigDecimal.valueOf(0.02))
                .setScale(0, BigDecimal.ROUND_UP);
        return Math.max(fee.intValue(), 2000);
    }

    /**
     * Create commission record for the expert.
     */
    private void createCommissionRecord(PaymentTransaction payment, Consultation consultation) {
        double commissionRate = consultation.getCommissionRateSnapshot() != null ?
                consultation.getCommissionRateSnapshot() : 0.80;

        int commissionAmount = (int) (consultation.getPriceSnapshotAmount() * commissionRate);
        int gatewayFeePortion = (int) (payment.getGatewayFee() * commissionRate);

        CommissionRecord commission = CommissionRecord.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(consultation.getBookingId())
                .expertId(consultation.getExpertId())
                .originalPrice(consultation.getPriceSnapshotAmount())
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .gatewayFee(gatewayFeePortion)
                .refundAmount(0)
                .expertNetAmount(commissionAmount - gatewayFeePortion)
                .eligibleAt(Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS))
                .settlementStatus("PENDING")
                .build();

        commissionRepository.save(commission);
        log.debug("Commission record created: commissionId={}, expertId={}, bookingId={}",
                commission.getCommissionId(), consultation.getExpertId(), consultation.getBookingId());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(String bookingRef) {
        log.debug("Getting payment status for bookingRef: {}", bookingRef);

        Consultation consultation = consultationRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        PaymentTransaction payment = paymentRepository.findByBookingId(consultation.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking"));

        return paymentMapper.toPaymentResponse(payment, consultation.getSessionToken());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getPaymentTransaction(Long paymentId) {
        log.debug("Getting payment transaction: {}", paymentId);
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPaymentsByConsultation(Long bookingId) {
        log.debug("Getting payments for bookingId: {}", bookingId);
        Consultation consultation = consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        return paymentRepository.findByBookingId(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentTransaction> getPaymentsByUser(Long userId, String roleStr, Pageable pageable) {
        log.debug("Getting payments for user: {}, role: {}", userId, roleStr);

        // In a real implementation, this would filter by payer or expert based on role
        // For Sprint 0, return all payments for the consultation where user is involved
        List<PaymentTransaction> allPayments = paymentRepository.findAll();
        List<PaymentTransaction> userPayments = allPayments.stream()
                .filter(p -> p.getPayerUserId().equals(userId))
                .toList();

        // Simple pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), userPayments.size());
        List<PaymentTransaction> pageContent = start >= userPayments.size() ? List.of() : userPayments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, userPayments.size());
    }

    @Override
    @Transactional
    public RefundRecord issueRefund(Long paymentId, RefundRequest request) {
        log.info("Issuing refund for paymentId: {}, reason: {}", paymentId, request.getReason());

        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        // Check payment is completed
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot refund a payment that is not completed");
        }

        // Check refund amount valid
        if (request.getRefundAmount() > payment.getNetPaidAmount()) {
            throw new IllegalArgumentException("Refund amount cannot exceed net paid amount");
        }

        // Get consultation for policy check
        Consultation consultation = consultationRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        // TODO: Apply paymentPolicy.ensureCanIssueRefund when dispute integration is ready

        // Create refund record
        RefundRecord refund = RefundRecord.builder()
                .paymentId(paymentId)
                .disputeId(request.getDisputeId())
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason())
                .status(com.carebridge.backend.expert.enums.DisputeStatus.PENDING)
                .requestedAt(Instant.now())
                .processedAt(null)
                .build();

        refund = refundRepository.save(refund);

        // Update commission records
        CommissionRecord commission = commissionRepository.findByPaymentId(paymentId)
                .orElse(null);
        if (commission != null) {
            commission.setRefundAmount(commission.getRefundAmount() + request.getRefundAmount());
            commission.setSettlementStatus("PENDING"); // Re-evaluate settlement
            commissionRepository.save(commission);
        }

        log.info("Refund created: refundId={}, paymentId={}", refund.getRefundId(), paymentId);
        return refund;
    }
}
