package com.carebridge.backend.babycare.entity;

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
@Table(name = "vaccination_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vaccination_record_id")
    private UUID vaccinationRecordId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "vaccine_name", length = 200)
    private String vaccineName;

    @Column(name = "dose_number", length = 30)
    private String doseNumber;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "facility_name", length = 255)
    private String facilityName;

    @Column(name = "proof_record_id")
    private UUID proofRecordId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
