package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AddHealthRecordRequest {

    @NotNull
    private RecordType recordType;

    @NotBlank
    @Size(max = 255)
    private String title;

    private LocalDate recordDate;

    @Size(max = 200)
    private String facilityName;

    private UUID journeyId;

    private UUID babyId;

    private List<UUID> fileIds;
}
