package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerificationResponse {
    private UUID identityVerificationId;
    private UUID expertProfileId;
    private UUID selfieFileId;
    private UUID identityFrontFileId;
    private UUID identityBackFileId;
    private UUID selfieCropFileId;
    private UUID idCardCropFileId;
    private FaceVerificationStatus faceStatus;
    private BigDecimal faceSimilarity;
    private BigDecimal faceThreshold;
    private String providerErrorCode;
    private IdentityReviewStatus reviewStatus;
    private String reviewReason;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
}
