package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.UUID;
import com.carebridge.backend.triage.TriageStage;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunIntakeRequest {

    @Size(max = 2000, message = "symptoms must not exceed 2000 characters")
    @Pattern(regexp = ".*\\S.*", message = "symptoms must not be blank")
    private String symptoms;

    private Integer childAgeMonths;
    private List<String> symptomList;
    private String duration;
    private Double temperatureC;
    private String feedingStatus;
    private String breathingStatus;
    private String consciousnessStatus;

    @Size(max = 2000, message = "painSeverity must not exceed 2000 characters")
    private String painSeverity;

    @Size(max = 2000, message = "urinarySymptoms must not exceed 2000 characters")
    private String urinarySymptoms;

    @Size(max = 2000, message = "hydrationStatus must not exceed 2000 characters")
    private String hydrationStatus;

    private String vomiting;
    private String diarrhea;
    private String rash;
    private Boolean seizure;
    private List<String> dehydrationSigns;
    private String parentFreeText;
    private UUID babyProfileId;
    private UUID motherProfileId;
    private TriageStage stage;

    // CB-TRIAGE-MATQ-IMP-001: trusted, server-derived gestational week auto-bound from the
    // caller's active PREGNANCY journey (never user-supplied, never a risk-rule input).
    private Integer gestationalWeeks;

    @Size(max = 2000, message = "abdominalPainPattern must not exceed 2000 characters")
    private String abdominalPainPattern;
}
