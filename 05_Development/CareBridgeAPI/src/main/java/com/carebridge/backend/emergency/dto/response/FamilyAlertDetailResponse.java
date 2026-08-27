package com.carebridge.backend.emergency.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyAlertDetailResponse {

    private UUID sessionId;
    private String motherName;
    private String motherPhone;
    private String status;
    private String triggerSource;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean locationIncluded;
    private int recipientCount;
    private boolean acknowledged;
    private Instant acknowledgedAt;
    private Instant createdAt;
    private Instant resolvedAt;
}
