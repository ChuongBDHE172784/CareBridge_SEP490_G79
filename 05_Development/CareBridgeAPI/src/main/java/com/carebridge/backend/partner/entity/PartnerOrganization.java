package com.carebridge.backend.partner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "archived_partner_records",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_partner_representative", columnNames = {"representative_user_id"}),
            @UniqueConstraint(name = "uk_partner_email", columnNames = {"email"})
        }
)
@org.hibernate.annotations.SQLRestriction("legacy_table = 'partner_organizations'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 20)
    private OrganizationType type;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_status", nullable = false, length = 30)
    private OrganizationStatus status;

    @Column(name = "representative_user_id", nullable = false)
    private UUID representativeUserId;

    @CreationTimestamp
    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "partner_organizations";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "partner_organizations";
        legacyId = id.toString();
    }
}
