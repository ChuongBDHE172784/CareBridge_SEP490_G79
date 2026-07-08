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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
