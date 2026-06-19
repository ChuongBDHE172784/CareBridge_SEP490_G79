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
@Table(name = "expert_consultation_prices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertConsultationPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expert_price_id")
    private UUID expertPriceId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "price_band_id")
    private UUID priceBandId;

    @Column(name = "channel_type", length = 20)
    private String channelType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "price_amount")
    private BigDecimal priceAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
