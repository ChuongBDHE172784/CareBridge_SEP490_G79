package com.carebridge.backend.consultation.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.request.UpdateConsultationStatusRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.service.IAvailabilityService;
import com.carebridge.backend.payment.entity.ExpertConsultationPrice;
import com.carebridge.backend.payment.repository.ExpertConsultationPriceRepository;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Consultation Service Implementation.
 * Extends AbstractConsultationService and implements IConsultationService.
 *
 * SOLID:
 * - SRP: Handles consultation CRUD, delegates booking to BookingService
 * - LSP: Substitutable for AbstractConsultationService
 * - DIP: Depends on abstractions (repositories, services)
 */
@Service("consultationService")
@RequiredArgsConstructor
@Slf4j
public class ConsultationServiceImpl extends AbstractConsultationService<Consultation> implements IConsultationService {

    private final IAvailabilityService availabilityService;
    private final ExpertConsultationPriceRepository priceRepository;

    public ConsultationServiceImpl(com.carebridge.backend.consultation.repository.ConsultationRepository consultationRepository,
                                   ExpertRepository expertRepository,
                                   com.carebridge.backend.payment.repository.PaymentTransactionRepository paymentRepository,
                                   PaymentMapper paymentMapper,
                                   IAvailabilityService availabilityService,
                                   ExpertConsultationPriceRepository priceRepository) {
        super(consultationRepository, expertRepository, paymentRepository, paymentMapper, availabilityService);
        this.availabilityService = availabilityService;
        this.priceRepository = priceRepository;
    }

    @Override
    protected Consultation findConsultationById(Long bookingId) {
        return consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    @Override
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        // Delegate to BookingService (Single Responsibility)
        throw new UnsupportedOperationException("Use BookingService for creating consultations");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationSummaryDTO> getUserConsultations(Long userId, String roleStr, Pageable pageable) {
        Role role = Role.valueOf(roleStr);

        List<Consultation> consultations;
        if (role == Role.EXPERT) {
            consultations = consultationRepository.findByExpertId(userId);
        } else {
            consultations = consultationRepository.findByRequesterUserId(userId);
        }

        List<ConsultationSummaryDTO> dtos = consultations.stream()
                .map(consultation -> {
                    Expert expert = expertRepository.findById(consultation.getExpertId()).orElse(null);
                    return buildSummaryDTO(consultation, expert);
                })
                .toList();

        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    @Override
    @Transactional
    public void cancelConsultation(Long bookingId, Long userId, String roleStr, String reason) {
        Consultation consultation = loadConsultationWithPolicy(bookingId, userId, Role.valueOf(roleStr));

        if (consultation.getStatus() != ConsultationStatus.PENDING_PAYMENT &&
                consultation.getStatus() != ConsultationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel consultation in current status: " + consultation.getStatus());
        }

        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultationRepository.save(consultation);
        log.info("Consultation cancelled: {}, reason: {}", consultation.getBookingRef(), reason);
    }

    @Override
    @Transactional
    public ConsultationDetailDTO rescheduleConsultation(Long bookingId,
                                                         Long userId,
                                                         String role,
                                                         UpdateConsultationStatusRequest request) {
        throw new UnsupportedOperationException("Rescheduling not implemented in Sprint 0");
    }
}
