package com.carebridge.backend.expert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professional_specialties")
@IdClass(ProfessionalSpecialtyId.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSpecialty {

    @Id
    @Column(name = "professional_profile_id", nullable = false, updatable = false)
    private UUID professionalProfileId;

    @Id
    @Column(name = "specialty_id", nullable = false, updatable = false)
    private UUID specialtyId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
