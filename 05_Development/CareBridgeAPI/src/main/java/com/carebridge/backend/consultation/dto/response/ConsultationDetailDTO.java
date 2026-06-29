package com.carebridge.backend.consultation.dto.response;

import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Detailed consultation information.
 * Maps to consultation_bookings entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDetailDTO {

    private Long bookingId;
    private String bookingRef;
    private Long expertId;
    private ExpertInfo expert;
    private Long requesterUserId;
    private ConsultationModality modality;
    private Integer durationMinutes;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private ConsultationStatus status;
    private Integer priceSnapshotAmount;
    private String currency;
    private String sessionToken;
    private String expertSummary;
    private String disputeStatus;
    private RealtimeSessionInfo realtimeSession;
    private PaymentInfo payment;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertInfo {
        private Long expertId;
        private String specialty;
        private String professionalTitle;
        private String workplace;
        private Double averageRating;
        private Integer reviewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeSessionInfo {
        private Long sessionId;
        private String roomId;
        private String providerType;
        private String sessionStatus;
        private Instant startedAt;
        private Instant endedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private Long paymentId;
        private String gatewayName;
        private String gatewayTransactionId;
        private Integer grossAmount;
        private Integer gatewayFee;
        private Integer netPaidAmount;
        private String status;
        private Instant paidAt;
    }
}
