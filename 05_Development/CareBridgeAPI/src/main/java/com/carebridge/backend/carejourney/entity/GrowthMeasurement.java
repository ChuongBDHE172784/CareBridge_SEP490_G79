package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "growth_measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "growth_measurement_id", updatable = false, nullable = false)
    private UUID growthMeasurementId;

    /**
     * Legacy identifier column. The canonical growth_measurements relation keeps both
     * baby_id and care_subject_id as NOT NULL columns; {@link #alignCanonicalCareSubject()}
     * mirrors the two fields so either write path fills both columns.
     */
    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    @Column(name = "measured_date", nullable = false)
    private LocalDate measuredDate;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "head_circumference_cm", precision = 5, scale = 2)
    private BigDecimal headCircumferenceCm;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void alignCanonicalCareSubject() {
        if (careSubjectId == null && babyId != null) {
            careSubjectId = babyId;
        }
        if (babyId == null && careSubjectId != null) {
            babyId = careSubjectId;
        }
    }
}

