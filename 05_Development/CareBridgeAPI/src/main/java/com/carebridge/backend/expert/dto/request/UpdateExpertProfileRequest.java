package com.carebridge.backend.expert.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpertProfileRequest {

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 20, message = "Expertise areas max 20 items")
    private java.util.List<String> expertiseAreas;

    @Positive(message = "Years of experience must be positive")
    private Integer yearsExperience;

    @Size(max = 2000, message = "Qualifications must not exceed 2000 characters")
    private String qualifications;

    @Positive(message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;
}
