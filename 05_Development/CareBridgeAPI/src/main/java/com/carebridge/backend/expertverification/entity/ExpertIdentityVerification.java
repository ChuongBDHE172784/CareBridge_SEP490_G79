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
@Table(name = "expert_identity_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertIdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "identity_verification_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "selfie_file_id", nullable = false)
    private UUID selfieFileId;

    @Column(name = "identity_front_file_id", nullable = false)
    private UUID identityFrontFileId;

    @Column(name = "identity_back_file_id", nullable = false)
    private UUID identityBackFileId;

    @Column(name = "face_provider", nullable = false, length = 30)
    private String faceProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "face_status", nullable = false, length = 40)
    private FaceVerificationStatus faceStatus;

    @Column(name = "face_similarity", precision = 7, scale = 6)
    private BigDecimal faceSimilarity;

    @Column(name = "face_threshold", precision = 7, scale = 6)
    private BigDecimal faceThreshold;

    @Column(name = "provider_error_code", length = 100)
    private String providerErrorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 40)
    private IdentityReviewStatus reviewStatus;

    @Column(name = "review_reason", columnDefinition = "text")
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
}
