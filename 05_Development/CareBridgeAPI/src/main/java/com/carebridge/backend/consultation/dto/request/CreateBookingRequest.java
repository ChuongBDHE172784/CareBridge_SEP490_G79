package com.carebridge.backend.consultation.dto.request;

import com.carebridge.backend.expert.enums.ConsultationModality;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for creating a consultation booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    /**
     * Expert to book with.
     */
    @NotNull(message = "Expert ID is required")
    private Long expertId;

    /**
     * Availability slot ID to book.
     */
    @NotNull(message = "Availability slot ID is required")
    private Long availabilityId;

    /**
     * Consultation modality.
     */
    @NotNull(message = "Channel type is required")
    private ConsultationModality modality;

    /**
     * Brief summary of the concern.
     */
    private String concernSummary;
}
