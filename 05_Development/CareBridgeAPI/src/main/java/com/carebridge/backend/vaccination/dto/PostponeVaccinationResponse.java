package com.carebridge.backend.vaccination.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PostponeVaccinationResponse {
    private UUID vaccinationRecordId;
    private UUID babyId;
    private String vaccineName;
    private Short doseNumber;
    private LocalDate previousScheduledDate;
    private LocalDate newScheduledDate;
    private String status;
    private String reason;
    private boolean created;
}
