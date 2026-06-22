package com.carebridge.backend.expert.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpertProfileRequest {

    @NotEmpty(message = "Expertise areas are required")
    @Size(min = 1, max = 20, message = "Must have at least 1 and max 20 expertise areas")
    private List<String> expertiseAreas;

    @NotNull(message = "Years of experience is required")
    @Positive(message = "Years of experience must be positive")
    private Integer yearsExperience;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 2000, message = "Qualifications must not exceed 2000 characters")
    private String qualifications;

    @NotNull(message = "Hourly rate is required")
    @Positive(message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;
}
