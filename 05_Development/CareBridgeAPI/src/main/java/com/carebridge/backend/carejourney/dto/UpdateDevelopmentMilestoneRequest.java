package com.carebridge.backend.carejourney.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateDevelopmentMilestoneRequest {

    private LocalDate achievedDate;
    private String note;
    private String status;
}
