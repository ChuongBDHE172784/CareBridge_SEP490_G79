package com.carebridge.backend.expertverification.entity;

import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expert_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id", updatable = false, nullable = false)
    private UUID credentialId;

    @Column(name = "professional_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "credential_type", nullable = false, length = 50)
    private String credentialType;

    @Column(name = "credential_number", length = 100)
    private String credentialNumber;

    @Column(name = "issuer", length = 200)
    private String issuer;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_url", columnDefinition = "text")
    private String fileUrl;

    @Column(name = "file_id")
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private ReviewStatus reviewStatus;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
