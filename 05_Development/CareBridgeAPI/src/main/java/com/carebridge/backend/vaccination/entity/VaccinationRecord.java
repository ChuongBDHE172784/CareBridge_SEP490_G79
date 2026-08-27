package com.carebridge.backend.vaccination.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vaccination_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vaccination_record_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    /**
     * {@code vaccination_records.care_subject_id} is NOT NULL in the canonical schema and
     * carries the same care-subject identity as {@code baby_id} (both FK to
     * {@code care_subjects.care_subject_id}). It is mirrored in {@link #syncCareSubject()}
     * so callers keep working with {@code babyId} alone.
     */
    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    /** Catalogue dose this record was materialised from; null for manually added records. */
    @Column(name = "vaccination_schedule_id")
    private UUID vaccinationScheduleId;

    @Column(name = "vaccine_name", nullable = false, length = 200)
    private String vaccineName;

    @Column(name = "dose_number")
    private Short doseNumber;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VaccinationRecordStatus status = VaccinationRecordStatus.SCHEDULED;

    @Column(name = "facility_name", length = 200)
    private String facilityName;

    @Column(name = "proof_record_id")
    private UUID proofRecordId;

    @Column(name = "postpone_reason", columnDefinition = "text")
    private String postponeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void syncCareSubject() {
        if (careSubjectId == null) {
            careSubjectId = babyId;
        }
    }
}
