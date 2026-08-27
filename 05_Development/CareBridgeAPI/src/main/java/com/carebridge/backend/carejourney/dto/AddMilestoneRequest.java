package com.carebridge.backend.carejourney.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AddMilestoneRequest {

    @NotNull(message = "Milestone type is required")
    private String milestoneType;

    @NotNull(message = "Achieved date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate achievedDate;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    private String sourceType;
}
