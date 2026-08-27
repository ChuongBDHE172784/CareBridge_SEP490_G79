package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationStatusResponse {
    private VerificationStatus status;
    private LocalDateTime verifiedAt;
    private UUID verifiedBy;
    private String rejectionReason;
    private boolean canRenew;
    private String nextStep;
}
