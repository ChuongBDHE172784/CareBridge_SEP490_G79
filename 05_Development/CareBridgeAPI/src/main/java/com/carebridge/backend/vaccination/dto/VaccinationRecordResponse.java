package com.carebridge.backend.vaccination.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class VaccinationRecordResponse {
    private UUID id;
    private UUID babyId;
    private String vaccineName;
    private Short doseNumber;
    private LocalDate scheduledDate;
    private LocalDate administeredDate;
    private String status;
    private String facilityName;
    private UUID proofRecordId;
    private Instant createdAt;
    private Instant updatedAt;
}
