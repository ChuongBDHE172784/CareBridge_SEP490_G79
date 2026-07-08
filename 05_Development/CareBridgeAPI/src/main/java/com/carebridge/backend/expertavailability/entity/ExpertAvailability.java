package com.carebridge.backend.expertavailability.entity;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expert_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "availability_id", updatable = false, nullable = false)
    private UUID availabilityId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "channel_type", nullable = false, length = 30)
    private String channelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
