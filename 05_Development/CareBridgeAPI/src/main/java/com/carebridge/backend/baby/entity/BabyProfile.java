package com.carebridge.backend.baby.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "baby_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BabyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "baby_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "related_journey_id")
    private UUID relatedJourneyId;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 10)
    private Gender gender;

    @Column(name = "birth_weight_kg", precision = 4, scale = 2)
    private BigDecimal birthWeightKg;

    @Column(name = "birth_length_cm", precision = 4, scale = 1)
    private BigDecimal birthLengthCm;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BabyProfileStatus status = BabyProfileStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
