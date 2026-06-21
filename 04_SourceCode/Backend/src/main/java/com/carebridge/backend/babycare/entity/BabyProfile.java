package com.carebridge.backend.babycare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "related_journey_id")
    private UUID relatedJourneyId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "sex", length = 20)
    private String sex;

    @Column(name = "birth_weight_kg")
    private BigDecimal birthWeightKg;

    @Column(name = "birth_length_cm")
    private BigDecimal birthLengthCm;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
