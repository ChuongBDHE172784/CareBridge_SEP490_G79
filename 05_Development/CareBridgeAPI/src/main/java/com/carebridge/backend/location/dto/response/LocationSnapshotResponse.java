package com.carebridge.backend.location.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSnapshotResponse {
    private UUID locationSnapshotId;
    private UUID userId;
    private String contextType;
    private UUID contextId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;
    private String consentStatus;
}
