package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mother_journeys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotherJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "journey_type", length = 30)
    private String journeyType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "last_menstrual_date")
    private LocalDate lastMenstrualDate;

    @Column(name = "estimated_due_date")
    private LocalDate estimatedDueDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
