package com.carebridge.backend.expertavailability.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareResponse {
    private UUID locationShareId;
    private UUID expertProfileId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private String availabilityStatus;
    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    private UUID consentReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
