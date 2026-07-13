package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.UUID;

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
    private String vomiting;
    private String diarrhea;
    private String rash;
    private Boolean seizure;
    private List<String> dehydrationSigns;
    private String parentFreeText;
    private UUID babyProfileId;
}
