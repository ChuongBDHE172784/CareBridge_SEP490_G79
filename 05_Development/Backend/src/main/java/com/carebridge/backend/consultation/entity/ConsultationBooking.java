package com.carebridge.backend.consultation.entity;

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
@Table(name = "consultation_bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "requester_user_id")
    private UUID requesterUserId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "availability_id")
    private UUID availabilityId;

    @Column(name = "expert_price_id")
    private UUID expertPriceId;

    @Column(name = "shared_summary_id")
    private UUID sharedSummaryId;

    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "channel_type", length = 20)
    private String channelType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Column(name = "price_snapshot_amount")
    private BigDecimal priceSnapshotAmount;

    @Column(name = "commission_rate_snapshot")
    private BigDecimal commissionRateSnapshot;

    @Column(name = "currency")
    private String currency;

    @Column(name = "cancellation_policy_snapshot", columnDefinition = "TEXT")
    private String cancellationPolicySnapshot;

    @Column(name = "price_locked_at")
    private Instant priceLockedAt;

    @Column(name = "status", nullable = false, length = 25)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
