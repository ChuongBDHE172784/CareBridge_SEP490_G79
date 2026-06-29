package com.carebridge.backend.consultation.dto.response;

import com.carebridge.backend.expert.enums.ConsultationModality;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Consultation summary for list view.
 * Maps to consultation_bookings entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSummaryDTO {

    private Long bookingId;
    private String bookingRef;
    private Long expertId;
    private String expertSpecialty;
    private String expertName;
    private ConsultationModality modality;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private ConsultationStatus status;
    private Integer priceAmount;
    private String currency;
    private String sessionToken;
}
