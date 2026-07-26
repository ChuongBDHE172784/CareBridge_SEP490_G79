package com.carebridge.backend.expertverification.entity;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertIdentityVerification {

    private UUID id;

    private UUID expertProfileId;

    private UUID selfieFileId;

    private UUID identityFrontFileId;

    private UUID identityBackFileId;

    @Builder.Default
    private String faceProvider = "COMPREFACE";

    @Builder.Default
    private String faceStatus = FaceVerificationStatus.DISABLED.name();

    private BigDecimal faceSimilarity;

    private BigDecimal faceThreshold;

    private String providerErrorCode;

    private IdentityReviewStatus reviewStatus;

    private String reviewReason;

    private UUID reviewedBy;

    private Instant reviewedAt;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID selfieCropFileId;

    private UUID idCardCropFileId;

    private String detectionSelfieStatus;

    private String detectionIdCardStatus;

    private String pipelineErrorCode;

    private String pipelineStatus;

    private Instant processedAt;
}
