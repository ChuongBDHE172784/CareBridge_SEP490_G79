package com.carebridge.backend.consultation.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.entity.AvailabilitySlot;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.payment.entity.ExpertConsultationPrice;
import com.carebridge.backend.payment.repository.ExpertConsultationPriceRepository;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Booking Service Implementation.
 * Handles consultation booking creation and management.
 *
 * Extends AbstractConsultationService for common CRUD operations.
 * Implements IBookingService for booking-specific contracts.
 *
 * SOLID:
 * - SRP: Only handles booking creation, not consultation queries
 * - OCP: Can extend booking strategies via Template Methods
 * - LSP: Substitutable for AbstractConsultationService
 *
 * TV4 Use Cases: 3.3.1.52 (Book Private Consultation)
 */
@Service("bookingService")
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl extends AbstractConsultationService<Consultation> implements IBookingService {

    private final ExpertConsultationPriceRepository priceRepository;

    public BookingServiceImpl(com.carebridge.backend.consultation.repository.ConsultationRepository consultationRepository,
                              ExpertRepository expertRepository,
                              com.carebridge.backend.payment.repository.PaymentTransactionRepository paymentRepository,
                              PaymentMapper paymentMapper,
                              IAvailabilityService availabilityService,
                              ExpertConsultationPriceRepository priceRepository) {
        super(consultationRepository, expertRepository, paymentRepository, paymentMapper, availabilityService);
        this.priceRepository = priceRepository;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        log.info("Creating booking for userId: {}, expertId: {}", userId, request.getExpertId());

        // Validate expert exists and is approved
        Expert expert = expertRepository.findById(request.getExpertId())
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        if (expert.getVerificationStatus() != com.carebridge.backend.expert.enums.ExpertVerificationStatus.APPROVED) {
            throw new IllegalStateException("Expert is not approved for consultations");
        }

        // Get and validate availability slot
        var slot = availabilityService.getSlot(request.getAvailabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (slot.getExpertId() != request.getExpertId()) {
            throw new IllegalArgumentException("Slot does not belong to the specified expert");
        }

        if (!availabilityService.isSlotAvailable(request.getAvailabilityId())) {
            throw new ResourceAlreadyExistsException("Slot is no longer available");
        }

        // Check for double booking conflict
        boolean conflict = consultationRepository.existsConflict(
                request.getExpertId(),
                slot.getSlotStart(),
                slot.getSlotEnd(),
                null);
        if (conflict) {
            throw new ResourceAlreadyExistsException("Expert already has a booking at this time");
        }

        // Get expert's price for this modality/duration
        int duration = calculateDuration(slot.getSlotStart(), slot.getSlotEnd());
        ExpertConsultationPrice price = priceRepository.findActivePrice(
                        request.getExpertId(),
                        request.getModality().name(),
                        duration)
                .orElseThrow(() -> new IllegalStateException("Expert has not set pricing for this consultation type"));

        // Generate booking reference (Hook method - can be overridden)
        String bookingRef = generateBookingRef();

        // Create consultation entity (Hook for customization)
        Consultation consultation = buildConsultationEntity(request, expert, slot, price, bookingRef);

        consultation = consultationRepository.save(consultation);
        log.info("Created booking: {}", consultation.getBookingRef());

        // Mark slot as booked - use bookingId
        availabilityService.markAsBooked(request.getAvailabilityId(), consultation.getBookingId());

        // Build response
        return buildBookingResponse(consultation, expert);
    }

    /**
     * Hook: Build consultation entity - can be overridden for custom logic.
     */
    protected Consultation buildConsultationEntity(CreateBookingRequest request,
                                                    Expert expert,
                                                    AvailabilitySlot slot,
                                                    ExpertConsultationPrice price,
                                                    String bookingRef) {
        return Consultation.builder()
                .bookingRef(bookingRef)
                .expertId(request.getExpertId())
                .requesterUserId(request.getUserId() != null ? request.getUserId() : 0L) // Will be set from auth
                .availabilityId(request.getAvailabilityId())
                .expertPriceId(price.getExpertPriceId())
                .channelType(request.getModality().name())
                .durationMinutes(calculateDuration(slot.getSlotStart(), slot.getSlotEnd()))
                .scheduledStart(slot.getSlotStart())
                .scheduledEnd(slot.getSlotEnd())
                .status(ConsultationStatus.PENDING_PAYMENT)
                .priceSnapshotAmount(price.getPriceAmount())
                .currency(price.getCurrency())
                .cancellationPolicy(price.getCancellationPolicy())
                .commissionRateSnapshot(calculateCommissionRate(price))
                .priceLockedAt(Instant.now())
                .build();
    }

    /**
     * Hook: Calculate commission rate - can be customized.
     */
    protected double calculateCommissionRate(ExpertConsultationPrice price) {
        // Default: 80% to expert
        return 0.80;
    }

    /**
     * Hook: Build booking response - can be customized.
     */
    protected BookingResponse buildBookingResponse(Consultation consultation, Expert expert) {
        BookingResponse.PaymentInfo paymentInfo = BookingResponse.PaymentInfo.builder()
                .qrCodeUrl("https://mock.vnpay.vn/qr/" + UUID.randomUUID())
                .paymentUrl("https://mock.vnpay.vn/pay/" + UUID.randomUUID())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .transactionToken("mock-txn-" + UUID.randomUUID())
                .build();

        return BookingResponse.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expert(BookingResponse.ExpertSummary.builder()
                        .expertId(expert.getExpertId())
                        .specialty(expert.getSpecialty())
                        .professionalTitle(expert.getProfessionalTitle())
                        .build())
                .scheduledTime(consultation.getScheduledStart())
                .modality(ConsultationModality.valueOf(consultation.getChannelType()))
                .price(consultation.getPriceSnapshotAmount())
                .status(ConsultationStatus.PENDING_PAYMENT)
                .payment(paymentInfo)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationSummaryDTO> getUserConsultations(Long userId, Role role) {
        throw new UnsupportedOperationException("Use ConsultationService for queries");
    }

    @Override
    protected Consultation findConsultationById(Long bookingId) {
        return consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }
}
