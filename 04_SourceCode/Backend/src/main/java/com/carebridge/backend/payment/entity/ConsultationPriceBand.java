package com.carebridge.backend.payment.entity;

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
@Table(name = "consultation_price_bands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPriceBand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "price_band_id")
    private UUID priceBandId;

    @Column(name = "configured_by")
    private UUID configuredBy;

    @Column(name = "channel_type", length = 20)
    private String channelType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "specialty_scope", length = 150)
    private String specialtyScope;

    @Column(name = "minimum_price")
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price")
    private BigDecimal maximumPrice;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
