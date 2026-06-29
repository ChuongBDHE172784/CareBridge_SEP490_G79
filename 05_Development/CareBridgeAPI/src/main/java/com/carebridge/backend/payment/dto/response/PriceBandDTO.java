package com.carebridge.backend.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Price band DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBandDTO {

    private Long priceBandId;
    private Long configuredBy;
    private String channelType;
    private Integer durationMinutes;
    private String specialtyScope;
    private Integer minimumPrice;
    private Integer maximumPrice;
    private Double commissionRate;
    private String currency;
    private String status;
}
