package com.carebridge.backend.vaccination.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class UpdateVaccinationRecordRequest {
    @Size(max = 200)
    private String vaccineName;

    @Min(1)
    private Short doseNumber;

    private LocalDate administeredDate;

    @Size(max = 200)
    private String facilityName;

    private UUID proofRecordId;
    private boolean clearProof;

    private final Set<String> unknownFields = new HashSet<>();

    @JsonAnySetter
    void captureUnknownField(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean attemptsStatusChange() {
        return unknownFields.contains("status");
    }
}
