package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertOnboardingResponse {
    private boolean profileExists;
    private String identityStatus;
    private String credentialStatus;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private String nextStep;
    private IdentityVerificationResponse latestIdentityAttempt;
}
