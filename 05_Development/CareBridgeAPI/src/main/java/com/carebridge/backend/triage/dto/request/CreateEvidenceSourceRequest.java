package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEvidenceSourceRequest {
    @NotBlank
    @Size(max = 500)
    private String baseUrl;

    @NotBlank
    @Size(max = 255)
    private String organization;

    @Size(max = 40)
    private String category;

    @Size(max = 200)
    private String applicableStages;

    @Size(max = 1000)
    private String notes;
}
