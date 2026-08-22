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
    /** COMMUNITY | PENDING_CONTRACT | CONTRACTED | null (chưa chọn hình thức). */
    private String expertType;
    private String rejectionReason;
    private String nextStep;
    private IdentityVerificationResponse latestIdentityAttempt;
}
