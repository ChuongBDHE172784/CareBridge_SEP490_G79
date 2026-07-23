package com.carebridge.backend.consent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "data_permissions")
@org.hibernate.annotations.SQLRestriction("permission_kind = 'CONSENT_GRANT'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_consent_id")
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private java.util.UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 60)
    private ConsentDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private ConsentPurpose purpose;

    @Column(length = 120)
    private String recipient;

    @Column(name = "scope_text", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "policy_version", length = 60)
    private String policyVersion;

    @Column(name = "evidence_key")
    private java.util.UUID evidenceKey;

    @Column(length = 20)
    private String locale;

    @Column(name = "granted_at", nullable = false)
    private Instant consentGivenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiryAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private java.util.UUID revokedBy;

    @Builder.Default
    @Column(name = "version_number", nullable = false)
    private int version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "permission_kind", nullable = false, updatable = false)
    private String permissionKind = "CONSENT_GRANT";

    @PrePersist
    void prepareCanonicalPermission() {
        permissionKind = "CONSENT_GRANT";
    }
}
