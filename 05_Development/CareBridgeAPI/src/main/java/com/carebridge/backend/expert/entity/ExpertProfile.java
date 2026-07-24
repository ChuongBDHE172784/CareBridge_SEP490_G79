package com.carebridge.backend.expert.entity;

import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Types;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "professional_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "professional_profile_id", updatable = false, nullable = false)
    private UUID expertProfileId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "professional_title", length = 150)
    private String professionalTitle;

    @Column(name = "experience_years", columnDefinition = "smallint")
    @JdbcTypeCode(Types.SMALLINT)
    private Integer experienceYears;

    @Column(name = "workplace", length = 200)
    private String workplace;

    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "consultation_scope", columnDefinition = "text")
    private String consultationScope;

    @Column(name = "consultation_fee_vnd")
    private Long consultationFeeVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trust_status", nullable = false, length = 20)
    private TrustStatus trustStatus = TrustStatus.ACTIVE;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isEligibleForConsultation() {
        return verificationStatus == VerificationStatus.APPROVED
                && trustStatus == TrustStatus.ACTIVE;
    }
}
