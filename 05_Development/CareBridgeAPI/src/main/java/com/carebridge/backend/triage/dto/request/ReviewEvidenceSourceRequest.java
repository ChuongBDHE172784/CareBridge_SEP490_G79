package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewEvidenceSourceRequest {
    @Size(max = 1000)
    private String notes;
}
