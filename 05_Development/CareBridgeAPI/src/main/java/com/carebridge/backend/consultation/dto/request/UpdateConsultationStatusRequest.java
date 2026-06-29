package com.carebridge.backend.consultation.dto.request;

import com.carebridge.backend.expert.enums.ConsultationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating consultation status.
 * Used for cancellations, rescheduling, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConsultationStatusRequest {

    /**
     * New status.
     */
    @NotNull(message = "Status is required")
    private ConsultationStatus status;

    /**
     * Reason for status change (e.g., cancellation reason).
     */
    private String reason;

    /**
     * Optional new scheduled time for rescheduling.
     */
    private java.time.Instant rescheduledTime;
}
