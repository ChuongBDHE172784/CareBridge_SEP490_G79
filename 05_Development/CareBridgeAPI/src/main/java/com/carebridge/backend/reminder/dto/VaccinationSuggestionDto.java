package com.carebridge.backend.reminder.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class VaccinationSuggestionDto {

    private UUID babyId;
    private String vaccineName;
    private short doseNumber;
    private LocalDate scheduledDate;
}
