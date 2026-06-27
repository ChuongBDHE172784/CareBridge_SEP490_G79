package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunIntakeRequest {

    @NotBlank(message = "symptoms must not be blank")
    @Size(max = 2000, message = "symptoms must not exceed 2000 characters")
    private String symptoms;

    private UUID babyProfileId;
}
