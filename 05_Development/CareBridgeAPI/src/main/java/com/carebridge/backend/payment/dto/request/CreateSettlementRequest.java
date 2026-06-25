package com.carebridge.backend.payment.dto.request;

import com.carebridge.backend.expert.enums.SettlementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a settlement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSettlementRequest {

    /**
     * Expert ID.
     */
    @NotNull(message = "Expert ID is required")
    private Long expertId;

    /**
     * Settlement period start date.
     */
    @NotNull(message = "Settlement period start is required")
    private LocalDate periodStart;

    /**
     * Settlement period end date.
     */
    @NotNull(message = "Settlement period end is required")
    private LocalDate periodEnd;

    /**
     * Settlement status.
     */
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;
}
