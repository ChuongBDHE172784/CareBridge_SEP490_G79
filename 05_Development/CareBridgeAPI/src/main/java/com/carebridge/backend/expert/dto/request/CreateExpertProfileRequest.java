package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.entity.ConsultationModality;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateExpertProfileRequest {

    @NotBlank(message = "EXP-001: displayName is required")
    @Size(max = 200, message = "EXP-001: displayName must be at most 200 characters")
    private String displayName;

    @Size(max = 2000, message = "EXP-001: bio must be at most 2000 characters")
    private String bio;

    @NotEmpty(message = "EXP-001: specialties is required")
    @Size(max = 20, message = "EXP-001: specialties must have at most 20 items")
    private List<String> specialties;

    @NotNull(message = "EXP-001: yearsOfExperience is required")
    @Min(value = 0, message = "EXP-001: yearsOfExperience must be >= 0")
    @Max(value = 50, message = "EXP-001: yearsOfExperience must be <= 50")
    private Integer yearsOfExperience;

    @NotNull(message = "EXP-001: consultationFeeVnd is required")
    @Min(value = 0, message = "EXP-001: consultationFeeVnd must be >= 0")
    private Long consultationFeeVnd;

    @NotEmpty(message = "EXP-001: consultationModalities is required")
    private List<ConsultationModality> consultationModalities;
}
