package com.carebridge.backend.consultation.service;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.response.ConsultationDetailDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.entity.ConsultationSession;
import com.carebridge.backend.consultation.policy.ConsultationPolicy;
import com.carebridge.backend.consultation.repository.ConsultationRepository;
import com.carebridge.backend.consultation.repository.ConsultationSessionRepository;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.service.AvailabilityService;
import com.carebridge.backend.payment.entity.PaymentTransaction;
import com.carebridge.backend.payment.mapper.PaymentMapper;
import com.carebridge.backend.payment.repository.PaymentTransactionRepository;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Consultation Service.
 * Business logic for consultation management.
 *
 * TV4 Use Cases: View Consultation List, View Consultation Detail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ExpertRepository expertRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationPolicy consultationPolicy;
    private final AvailabilityService availabilityService;
    private final PaymentMapper paymentMapper;

    /**
     * Get consultation by ID with full details.
     */
    @Transactional(readOnly = true)
    public ConsultationDetailDTO getConsultation(Long bookingId, Long userId, Role role) {
        log.debug("Getting consultation detail: bookingId={}", bookingId);

        Consultation consultation = consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        consultationPolicy.ensureCanViewConsultation(consultation, userId, role);

        Expert expert = expertRepository.findById(consultation.getExpertId())
                .orElse(null);

        Optional<ConsultationSession> sessionOpt = sessionRepository.findByBookingId(bookingId);
        Optional<PaymentTransaction> paymentOpt = paymentRepository.findByBookingId(bookingId);

        return paymentMapper.toDetailDTO(
                consultation,
                expert,
                sessionOpt.orElse(null),
                paymentOpt.map(p -> paymentMapper.toPaymentResponse(p, consultation.getSessionToken()))
                        .orElse(null)
        );
    }

    /**
     * Get consultations for a user.
     *
     * @param userId the user ID
     * @param role the user's role
     * @return list of consultation summaries
     */
    @Transactional(readOnly = true)
    public List<ConsultationSummaryDTO> getUserConsultations(Long userId, Role role) {
        log.debug("Getting consultations for userId: {}", userId);

        List<Consultation> consultations;
        if (role == Role.EXPERT) {
            consultations = consultationRepository.findByExpertId(userId);
        } else {
            consultations = consultationRepository.findByRequesterUserId(userId);
        }

        return consultations.stream()
                .map(consultation -> {
                    Expert expert = expertRepository.findById(consultation.getExpertId()).orElse(null);
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
                })
                .toList();
    }

    /**
     * Cancel a consultation booking.
     * Only allowed for PENDING_PAYMENT or CONFIRMED statuses.
     */
    @Transactional
    public void cancelConsultation(Long bookingId, Long userId, Role role, String reason) {
        log.info("Cancelling consultation: bookingId={}, userId={}", bookingId, userId);

        Consultation consultation = consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        consultationPolicy.ensureCanUpdateConsultation(consultation, userId, role);

        if (consultation.getStatus() != ConsultationStatus.PENDING_PAYMENT &&
                consultation.getStatus() != ConsultationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel consultation in current status");
        }

        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultationRepository.save(consultation);

        log.info("Consultation cancelled: {}", consultation.getBookingRef());
    }
}
