package com.carebridge.backend.consultation.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.entity.AvailabilitySlot;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.policy.ConsultationPolicy;
import com.carebridge.backend.consultation.repository.AvailabilitySlotRepository;
import com.carebridge.backend.consultation.repository.ConsultationRepository;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import com.carebridge.backend.expert.enums.SlotStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.service.AvailabilityService;
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
 * Booking Service.
 * Handles consultation booking creation and management.
 *
 * TV4 Use Cases: 3.3.1.52 (Book Private Consultation)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ConsultationRepository consultationRepository;
    private final ExpertRepository expertRepository;
    private final AvailabilitySlotRepository availabilityRepository;
    private final ExpertConsultationPriceRepository priceRepository;
    private final ConsultationPolicy consultationPolicy;
    private final AvailabilityService availabilityService;

    /**
     * Create a new consultation booking.
     * Validates expert exists, slot is available, and price is within bounds.
     *
     * @param userId the user making the booking
     * @param request the booking request
     * @return booking response with payment info
     */
    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        log.info("Creating booking for userId: {}, expertId: {}", userId, request.getExpertId());

        // Check conflict: user already has booking at same time
        // (skipped for Sprint 0 simplicity)

        Expert expert = expertRepository.findById(request.getExpertId())
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        // Verify expert is approved
        if (expert.getVerificationStatus() != com.carebridge.backend.expert.enums.ExpertVerificationStatus.APPROVED) {
            throw new IllegalStateException("Expert is not approved for consultations");
        }

        // Get and validate availability slot
        AvailabilitySlot slot = availabilityRepository.findById(request.getAvailabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (slot.getExpertId() != request.getExpertId()) {
            throw new IllegalArgumentException("Slot does not belong to the specified expert");
        }

        if (!availabilityService.isSlotAvailable(request.getAvailabilityId())) {
            throw new ResourceAlreadyExistsException("Slot is no longer available");
        }

        // Check for double booking on expert's side
        boolean conflict = consultationRepository.existsConflict(
                request.getExpertId(),
                slot.getSlotStart(),
                slot.getSlotEnd(),
                null);
        if (conflict) {
            throw new ResourceAlreadyExistsException("Expert already has a booking at this time");
        }

        // Get expert's price for this modality/duration
        int duration = ChronoUnit.MINUTES.between(slot.getSlotStart(), slot.getSlotEnd());
        ExpertConsultationPrice price = priceRepository.findActivePrice(
                        request.getExpertId(),
                        request.getModality().name(),
                        duration)
                .orElseThrow(() -> new IllegalStateException("Expert has not set pricing for this consultation type"));

        // Generate booking reference
        String bookingRef = generateBookingRef();

        // Create consultation
        Consultation consultation = Consultation.builder()
                .bookingRef(bookingRef)
                .expertId(request.getExpertId())
                .requesterUserId(userId)
                .availabilityId(request.getAvailabilityId())
                .channelType(request.getModality().name())
                .durationMinutes(duration)
                .scheduledStart(slot.getSlotStart())
                .scheduledEnd(slot.getSlotEnd())
                .status(ConsultationStatus.PENDING_PAYMENT)
                .priceSnapshotAmount(price.getPriceAmount())
                .currency(price.getCurrency())
                .cancellationPolicy(price.getCancellationPolicy())
                .commissionRateSnapshot(price.getPriceAmount() > 0 ?
                        (double) price.getPriceAmount() * 0.8 / price.getPriceAmount() : 0.0)
                .priceLockedAt(Instant.now())
                .build();

        consultation = consultationRepository.save(consultation);
        log.info("Created consultation: {}", consultation.getBookingRef());

        // Mark slot as booked
        availabilityService.markAsBooked(request.getAvailabilityId(), consultation.getBookingId());

        // Build mock payment response (Sprint 0 - no real payment)
        BookingResponse.PaymentInfo paymentInfo = BookingResponse.PaymentInfo.builder()
                .qrCodeUrl("https://mock.vnpay.vn/qr/" + UUID.randomUUID())
                .paymentUrl("https://mock.vnpay.vn/pay/" + UUID.randomUUID())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .transactionToken("mock-txn-" + UUID.randomUUID())
                .build();

        return BookingResponse.builder()
                .bookingRef(consultation.getBookingRef())
                .expert(BookingResponse.ExpertSummary.builder()
                        .expertId(expert.getExpertId())
                        .specialty(expert.getSpecialty())
                        .professionalTitle(expert.getProfessionalTitle())
                        .build())
                .scheduledTime(consultation.getScheduledStart())
                .modality(request.getModality())
                .price(consultation.getPriceSnapshotAmount())
                .status(ConsultationStatus.PENDING_PAYMENT)
                .payment(paymentInfo)
                .build();
    }

    /**
     * Generate unique booking reference.
     * Format: BK-YYYYMMDD-XXXX
     */
    private String generateBookingRef() {
        String datePart = Instant.now().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BK-" + datePart + "-" + uuid;
    }
}
