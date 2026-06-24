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
@Table(name = "care_facilities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "facility_type", length = 40)
    private String facilityType;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "opening_hours_json", columnDefinition = "jsonb")
    private String openingHoursJson;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
