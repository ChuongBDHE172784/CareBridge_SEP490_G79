package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCareGroupRequest {

    @NotBlank
    @Size(max = 100)
    private String groupName;

    @Size(max = 500)
    private String description;

    private UUID linkedJourneyId;

    private UUID linkedBabyProfileId;
}
