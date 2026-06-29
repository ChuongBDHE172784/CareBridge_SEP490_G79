package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.enums.ExpertVerificationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for creating an expert profile.
 * Validated with Bean Validation annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpertRequest {

    /**
     * Medical specialty.
     * Required, max 100 characters.
     */
    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    private String specialty;

    /**
     * Years of professional experience.
     * Required, must be non-negative.
     */
    @NotNull(message = "Experience years is required")
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
     * Credentials for verification.
     */
    private CredentialsDTO credentials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialsDTO {
        /**
         * License number.
         */
        @Size(max = 200, message = "License number must not exceed 200 characters")
        private String licenseNumber;

        /**
         * License expiry date.
         */
        private Instant licenseExpiry;

        /**
         * Certifications (array of IDs or codes).
         */
        private java.util.List<String> certifications;
    }
}
