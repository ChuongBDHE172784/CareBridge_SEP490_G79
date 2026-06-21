package com.carebridge.backend.expert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expert_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "credential_type", length = 50)
    private String credentialType;

    @Column(name = "credential_number", length = 120)
    private String credentialNumber;

    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "review_status", length = 20)
    private String reviewStatus;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
