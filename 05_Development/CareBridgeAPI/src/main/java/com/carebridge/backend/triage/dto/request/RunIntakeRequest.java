package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.util.List;
import java.util.UUID;
import com.carebridge.backend.triage.TriageStage;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunIntakeRequest {

    @Size(max = 2000, message = "symptoms must not exceed 2000 characters")
    @Pattern(regexp = ".*\\S.*", message = "symptoms must not be blank")
    private String symptoms;

    @Min(0) @Max(240)
    private Integer childAgeMonths;
    @Size(max = 32)
    private List<@Size(max = 120) String> symptomList;
    @Size(max = 120) private String duration;
    @DecimalMin("20.0") @DecimalMax("50.0") private Double temperatureC;
    @Size(max = 120) private String feedingStatus;
    @Size(max = 120) private String breathingStatus;
    @Size(max = 120) private String consciousnessStatus;

    @Size(max = 2000, message = "painSeverity must not exceed 2000 characters")
    private String painSeverity;

    @Size(max = 2000, message = "urinarySymptoms must not exceed 2000 characters")
    private String urinarySymptoms;

    @Size(max = 2000, message = "hydrationStatus must not exceed 2000 characters")
    private String hydrationStatus;

    @Size(max = 120) private String vomiting;
    @Size(max = 120) private String diarrhea;
    @Size(max = 120) private String rash;
    private Boolean seizure;
    @Size(max = 16) private List<@Size(max = 120) String> dehydrationSigns;
    @Size(max = 2000) private String parentFreeText;
    private UUID babyProfileId;
    private UUID motherProfileId;
    private TriageStage stage;

    // CB-TRIAGE-MATQ-IMP-001: trusted, server-derived gestational week auto-bound from the
    // caller's active PREGNANCY journey (never user-supplied, never a risk-rule input).
    @Min(0) @Max(45) private Integer gestationalWeeks;

    @Size(max = 2000, message = "abdominalPainPattern must not exceed 2000 characters")
    private String abdominalPainPattern;
}
