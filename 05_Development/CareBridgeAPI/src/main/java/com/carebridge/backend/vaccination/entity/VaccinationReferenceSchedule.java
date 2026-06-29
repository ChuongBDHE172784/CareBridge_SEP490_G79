package com.carebridge.backend.vaccination.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vaccination_reference_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccinationReferenceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ref_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vaccine_name", nullable = false, length = 200)
    private String vaccineName;

    @Column(name = "dose_number", nullable = false)
    private short doseNumber;

    @Column(name = "offset_days", nullable = false)
    private int offsetDays;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
