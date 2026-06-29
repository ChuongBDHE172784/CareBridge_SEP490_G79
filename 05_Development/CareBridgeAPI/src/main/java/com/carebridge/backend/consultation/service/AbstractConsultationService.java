package com.carebridge.backend.consultation.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.request.UpdateConsultationStatusRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationDetailDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.service.IAvailabilityService;
import com.carebridge.backend.payment.entity.PaymentTransaction;
import com.carebridge.backend.payment.mapper.PaymentMapper;
import com.carebridge.backend.payment.repository.PaymentTransactionRepository;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abstract base class for Consultation-related services.
 * Uses Strategy Pattern for booking and Template Method for CRUD operations.
 *
 * @param <T> the consultation entity type
 */
@Slf4j
public abstract class AbstractConsultationService<T extends Consultation> {

    protected final ConsultationRepository consultationRepository;
    protected final ExpertRepository expertRepository;
    protected final PaymentTransactionRepository paymentRepository;
    protected final PaymentMapper paymentMapper;
    protected final IAvailabilityService availabilityService;

    protected AbstractConsultationService(ConsultationRepository consultationRepository,
                                          ExpertRepository expertRepository,
                                          PaymentTransactionRepository paymentRepository,
                                          PaymentMapper paymentMapper,
                                          IAvailabilityService availabilityService) {
        this.consultationRepository = consultationRepository;
        this.expertRepository = expertRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.availabilityService = availabilityService;
    }

    // ============================================
    // Template Method: Get Consultation Detail
    // ============================================
    @Transactional(readOnly = true)
    public final ConsultationDetailDTO getConsultation(Long bookingId,
                                                        Long requestingUserId,
                                                        Role role) {
        T consultation = loadConsultationWithPolicy(bookingId, requestingUserId, role);
        Expert expert = expertRepository.findById(consultation.getExpertId()).orElse(null);
        PaymentTransaction payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        return buildConsultationDetailDTO(consultation, expert, payment);
    }

    /**
     * Load consultation with authorization check.
     */
    protected final T loadConsultationWithPolicy(Long bookingId,
                                                 Long requestingUserId,
                                                 Role role) {
        T consultation = findConsultationById(bookingId);
        enforceAuthorizationPolicy(consultation, requestingUserId, role);
        return consultation;
    }

    /**
     * Find consultation by ID - abstract to allow different entity types.
     */
    protected abstract T findConsultationById(Long bookingId);

    /**
     * Enforce authorization - can be overridden by subclasses.
     */
    protected void enforceAuthorizationPolicy(T consultation, Long requestingUserId, Role role) {
        if (!canAccessConsultation(consultation, requestingUserId, role)) {
            throw new com.carebridge.backend.common.exception.AccessDeniedBusinessException(
                    "Insufficient permissions to view consultation");
        }
    }

    /**
     * Check access permission.
     */
    protected boolean canAccessConsultation(T consultation, Long requestingUserId, Role role) {
        if (role == Role.SYSTEM_ADMIN) return true;
        if (role == Role.EXPERT && consultation.getExpertId().equals(requestingUserId)) return true;
        if (consultation.getRequesterUserId().equals(requestingUserId)) return true;
        return false;
    }

    /**
     * Build consultation detail DTO - hook method.
     */
    protected ConsultationDetailDTO buildConsultationDetailDTO(T consultation,
                                                                Expert expert,
                                                                PaymentTransaction payment) {
        ConsultationDetailDTO.ExpertInfo expertInfo = null;
        if (expert != null) {
            expertInfo = ConsultationDetailDTO.ExpertInfo.builder()
                    .expertId(expert.getExpertId())
                    .specialty(expert.getSpecialty())
                    .professionalTitle(expert.getProfessionalTitle())
                    .workplace(expert.getWorkplace())
                    .averageRating(expert.getRatingAvg())
                    .reviewCount(expert.getReviewCount())
                    .build();
        }

        ConsultationDetailDTO.PaymentInfo paymentInfo = null;
        if (payment != null) {
            paymentInfo = ConsultationDetailDTO.PaymentInfo.builder()
                    .paymentId(payment.getPaymentId())
                    .gatewayName(payment.getGatewayName())
                    .gatewayTransactionId(payment.getGatewayTransactionId())
                    .grossAmount(payment.getGrossAmount())
                    .gatewayFee(payment.getGatewayFee())
                    .netPaidAmount(payment.getNetPaidAmount())
                    .status(payment.getStatus() != null ?
                            com.carebridge.backend.expert.enums.PaymentStatus.valueOf(payment.getStatus().name()) : null)
                    .paidAt(payment.getPaidAt())
                    .build();
        }

        return ConsultationDetailDTO.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expertId(consultation.getExpertId())
                .expert(expertInfo)
                .requesterUserId(consultation.getRequesterUserId())
                .modality(consultation.getChannelType() != null ?
                        com.carebridge.backend.expert.enums.ConsultationModality.valueOf(consultation.getChannelType()) : null)
                .durationMinutes(consultation.getDurationMinutes())
                .scheduledStart(consultation.getScheduledStart())
                .scheduledEnd(consultation.getScheduledEnd())
                .status(consultation.getStatus() != null ?
                        com.carebridge.backend.expert.enums.ConsultationStatus.valueOf(consultation.getStatus().name()) : null)
                .priceSnapshotAmount(consultation.getPriceSnapshotAmount())
                .currency(consultation.getCurrency())
                .sessionToken(consultation.getSessionToken())
                .expertSummary(consultation.getExpertSummary())
                .disputeStatus(consultation.getDisputeStatus() != null ? consultation.getDisputeStatus().name() : null)
                .payment(paymentInfo)
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }

    /**
     * Map to summary DTO - hook method.
     */
    protected ConsultationSummaryDTO buildSummaryDTO(T consultation, Expert expert) {
        return ConsultationSummaryDTO.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expertId(consultation.getExpertId())
                .expertSpecialty(expert != null ? expert.getSpecialty() : null)
                .expertName(expert != null ? expert.getProfessionalTitle() : null)
                .modality(consultation.getChannelType() != null ?
                        com.carebridge.backend.expert.enums.ConsultationModality.valueOf(consultation.getChannelType()) : null)
                .scheduledStart(consultation.getScheduledStart())
                .scheduledEnd(consultation.getScheduledEnd())
                .status(consultation.getStatus() != null ?
                        com.carebridge.backend.expert.enums.ConsultationStatus.valueOf(consultation.getStatus().name()) : null)
                .priceAmount(consultation.getPriceSnapshotAmount())
                .currency(consultation.getCurrency())
                .sessionToken(consultation.getSessionToken())
                .build();
    }

    // ============================================
    // Abstract Business Methods (Strategy Pattern)
    // ============================================

    /**
     * Create booking - strategy varies by implementation.
     */
    public abstract BookingResponse createBooking(Long userId, CreateBookingRequest request);

    /**
     * Get user's consultations.
     */
    public abstract List<ConsultationSummaryDTO> getUserConsultations(Long userId, Role role);

    /**
     * Cancel consultation - strategy varies.
     */
    public abstract void cancelConsultation(Long bookingId, Long userId, Role role, String reason);

    /**
     * Helper to generate booking reference.
     */
    protected final String generateBookingRef() {
        String datePart = Instant.now().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BK-" + datePart + "-" + uuid;
    }

    /**
     * Calculate duration in minutes.
     */
    protected final int calculateDuration(Instant start, Instant end) {
        return (int) ChronoUnit.MINUTES.between(start, end);
    }
}
