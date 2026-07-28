package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExpertReviewCaseResponse {
    ExpertProfileResponse profile;
    IdentityVerificationResponse latestIdentity;
    List<DocumentReviewResponse> credentials;
    String identityStatus;
    String credentialStatus;
    boolean readyForFinalApproval;
}
