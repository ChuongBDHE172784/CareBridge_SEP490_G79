package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateHealthRecordRequest {

    @Size(max = 255)
    private String title;

    @Pattern(regexp = "ULTRASOUND|LAB_RESULT|PRESCRIPTION|VACCINATION_FORM|EXAMINATION_RESULT|NOTE",
             message = "Invalid record type")
    private String recordType;

    @PastOrPresent(message = "Record date cannot be in the future")
    private LocalDate recordDate;

    @Size(max = 30)
    private String sourceType;

    @Size(max = 200)
    private String sourceName;

    private String fileUrl;

    private UUID babyId;

    private UUID journeyId;

    private java.util.List<UUID> fileIds;
}
