package com.carebridge.backend.expert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expert_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "specialty", length = 150)
    private String specialty;

    @Column(name = "professional_title", length = 150)
    private String professionalTitle;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "workplace", length = 255)
    private String workplace;

    @Column(name = "consultation_scope", columnDefinition = "TEXT")
    private String consultationScope;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "rating_avg")
    private BigDecimal ratingAvg;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
