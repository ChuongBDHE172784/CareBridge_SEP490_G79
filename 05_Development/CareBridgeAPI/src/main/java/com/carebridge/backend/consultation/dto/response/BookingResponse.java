package com.carebridge.backend.consultation.dto.response;

import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for a consultation booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    /**
     * Booking ID (internal PK).
     */
    private Long bookingId;

    /**
     * Booking reference (human-readable).
     */
    private String bookingRef;

    /**
     * Expert information.
     */
    private ExpertSummary expert;

    /**
     * Scheduled start time.
     */
    private Instant scheduledTime;

    /**
     * Consultation modality.
     */
    private ConsultationModality modality;

    /**
     * Price in smallest currency unit.
     */
    private Integer price;

    /**
     * Current status.
     */
    private ConsultationStatus status;

    /**
     * Payment information (QR code for mock).
     */
    private PaymentInfo payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertSummary {
        private Long expertId;
        private String specialty;
        private String professionalTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String qrCodeUrl;
        private String paymentUrl;
        private Instant expiresAt;
        private String transactionToken;
    }
}
