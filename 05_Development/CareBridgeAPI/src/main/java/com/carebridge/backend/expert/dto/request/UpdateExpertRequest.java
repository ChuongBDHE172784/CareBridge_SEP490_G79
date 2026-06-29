package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.enums.ExpertVerificationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an expert profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpertRequest {

    /**
     * Medical specialty.
     */
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    private String specialty;

    /**
     * Years of professional experience.
     */
    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears;

    /**
     * Professional title.
     */
    @Size(max = 200, message = "Professional title must not exceed 200 characters")
    private String professionalTitle;

    /**
     * Workplace/hospital/clinic.
     */
    @Size(max = 300, message = "Workplace must not exceed 300 characters")
    private String workplace;

    /**
     * Consultation scope description.
     */
    private String consultationScope;

    /**
     * Internal admin note (admin only).
     */
    private String adminNote;
}
