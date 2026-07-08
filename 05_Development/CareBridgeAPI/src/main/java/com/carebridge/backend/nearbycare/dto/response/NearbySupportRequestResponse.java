package com.carebridge.backend.nearbycare.dto.response;

import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportRequestResponse {

    private UUID requestId;
    private UUID requesterUserId;
    private String supportType;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String consentStatus;
    private String status;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
