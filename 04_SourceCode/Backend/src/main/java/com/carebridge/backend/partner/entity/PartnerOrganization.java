package com.carebridge.backend.partner.entity;

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
@Table(name = "partner_organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "representative_user_id")
    private UUID representativeUserId;

    @Column(name = "partner_type", length = 30)
    private String partnerType;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "license_number", length = 120)
    private String licenseNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
