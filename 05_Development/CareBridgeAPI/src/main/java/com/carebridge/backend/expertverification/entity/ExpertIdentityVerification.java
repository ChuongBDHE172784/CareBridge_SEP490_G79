package com.carebridge.backend.expertverification.entity;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "expert_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertIdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "professional_profile_id", nullable = false)
    private UUID expertProfileId;

    @Builder.Default
    @Column(name = "credential_type", nullable = false, length = 50)
    private String credentialType = "IDENTITY_DOCUMENT";

    @Transient
    private UUID selfieFileId;

    @Transient
    private UUID identityFrontFileId;

    @Transient
    private UUID identityBackFileId;

    @Transient
    private String faceProvider;

    @Transient
    private FaceVerificationStatus faceStatus;

    @Transient
    private BigDecimal faceSimilarity;

    @Transient
    private BigDecimal faceThreshold;

    @Transient
    private String providerErrorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 40)
    private IdentityReviewStatus reviewStatus;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private UUID selfieCropFileId;

    @Transient
    private UUID idCardCropFileId;

    @Transient
    private String detectionSelfieStatus;

    @Transient
    private String detectionIdCardStatus;

    @Transient
    private String pipelineErrorCode;

    @Transient
    private String pipelineStatus;

    @Transient
    private Instant processedAt;
}
