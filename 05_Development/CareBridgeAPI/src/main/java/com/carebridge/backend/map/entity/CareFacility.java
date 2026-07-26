package com.carebridge.backend.map.entity;

import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "care_facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "facility_id", updatable = false, nullable = false)
    private UUID facilityId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "facility_type", length = 50)
    private String facilityType;

    @Column(name = "facility_level", length = 50)
    private String facilityLevel;

    @Column(name = "ownership_type", length = 30)
    private String ownershipType;

    @Column(length = 500)
    private String address;

    @Column(name = "province_id", length = 2)
    private String provinceId;

    @Column(name = "district_id", length = 4)
    private String districtId;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(length = 30)
    private String phone;

    @Column(name = "opening_hours_json", columnDefinition = "jsonb")
    private String openingHoursJson;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "external_source_id", length = 150)
    private String externalSourceId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "is_searchable", nullable = false)
    private Boolean searchable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private FacilityStatus verificationStatus = FacilityStatus.UNVERIFIED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
