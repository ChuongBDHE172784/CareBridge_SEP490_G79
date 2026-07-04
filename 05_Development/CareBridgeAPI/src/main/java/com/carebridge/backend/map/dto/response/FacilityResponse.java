package com.carebridge.backend.map.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityResponse {
    private UUID facilityId;
    private UUID partnerId;
    private String name;
    private String facilityType;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String openingHoursJson;
    private String sourceType;
    private String verificationStatus;
    private Integer distanceMeters;
}
