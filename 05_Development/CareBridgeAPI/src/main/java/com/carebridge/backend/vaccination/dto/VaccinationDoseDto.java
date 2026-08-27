package com.carebridge.backend.vaccination.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class VaccinationDoseDto {

    private String vaccineName;
    private int doseNumber;
    private LocalDate expectedDate;
    private LocalDate administeredDate;
    private String status;
}
