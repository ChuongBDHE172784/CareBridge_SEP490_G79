package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_record_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "summary_period", length = 30)
    private String summaryPeriod;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "record_date")
    private LocalDate periodEnd;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "source_name", length = 200)
    private String generatedBy;

    @Builder.Default
    @Column(name = "record_type", nullable = false, length = 50)
    private String recordType = "SUMMARY";

    @Builder.Default
    @Column(name = "title", nullable = false, length = 255)
    private String title = "Health summary";

    @Builder.Default
    @Column(name = "source_type", length = 30)
    private String sourceType = "SUMMARY";

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
