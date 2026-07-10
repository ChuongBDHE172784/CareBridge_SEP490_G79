package com.carebridge.backend.vaccination.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class VaccinationCompletionResponse {
    private UUID vaccinationRecordId;
    private UUID babyId;
    private String vaccineName;
    private Short doseNumber;
    private LocalDate administeredDate;
    private String status;
    private String facilityName;
    private UUID proofRecordId;
    private boolean created;
}
