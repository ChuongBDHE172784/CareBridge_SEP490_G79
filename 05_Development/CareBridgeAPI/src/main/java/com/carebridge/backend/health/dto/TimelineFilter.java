package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class TimelineFilter {

    @Pattern(regexp = "ULTRASOUND|LAB_RESULT|PRESCRIPTION|VACCINATION_FORM|EXAMINATION_RESULT|NOTE",
             message = "Invalid record type")
    private String recordType;

    private UUID journeyId;

    private UUID babyId;

    /** Required when a Family member opens records shared from one care group. */
    private UUID careGroupId;

    private String sourceType;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;
}
